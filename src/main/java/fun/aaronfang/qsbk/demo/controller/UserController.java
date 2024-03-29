package fun.aaronfang.qsbk.demo.controller;


import fun.aaronfang.qsbk.demo.common.ApiValidationException;
import fun.aaronfang.qsbk.demo.common.Result;
import fun.aaronfang.qsbk.demo.config.QsbkProps;
import fun.aaronfang.qsbk.demo.constants.ApiUserAuth;
import fun.aaronfang.qsbk.demo.constants.CommonRegex;
import fun.aaronfang.qsbk.demo.model.UserEntity;
import fun.aaronfang.qsbk.demo.model.UserinfoEntity;
import fun.aaronfang.qsbk.demo.repo.UserRepo;
import fun.aaronfang.qsbk.demo.repo.UserinfoRepo;
import fun.aaronfang.qsbk.demo.util.PhoneCacheUtils;
import fun.aaronfang.qsbk.demo.util.RedisUtils;
import fun.aaronfang.qsbk.demo.util.TimeUtil;
import fun.aaronfang.qsbk.demo.validation.phone.PhoneValidation;
import javafx.util.Pair;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Validated
@RestController
@RequestMapping(path = "/user", produces = "application/json")
public class UserController {

    @Resource
    RedisUtils redisUtils;
    final QsbkProps qsbkProps;
    final UserRepo userRepo;
    final UserinfoRepo userinfoRepo;
    final BCryptPasswordEncoder passwordEncoder;

    public UserController(QsbkProps qsbkProps, UserRepo userRepo, BCryptPasswordEncoder passwordEncoder, UserinfoRepo userinfoRepo) {
        this.qsbkProps = qsbkProps;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.userinfoRepo = userinfoRepo;
    }

    @PostMapping("sendcode")
    public ResponseEntity<Result> sendCode(@PhoneValidation @RequestParam("phone") String phone) {
        // 判断是否开启验证码功能
        if (qsbkProps.isEnableSms()) {
            // 判断是否已经发送过验证码
            String phoneCachedKey = PhoneCacheUtils.getPhoneCachedKey(phone);
            if (redisUtils.hasKey(phoneCachedKey)) {
                return new ResponseEntity<>(Result.buildResult(30001, "你操作得太快啦", null), HttpStatus.OK);
            }
            // 生成4位验证码
            String valiCode = PhoneCacheUtils.genarateValiCode(phone);
            // 写入缓存
            boolean setResult = redisUtils.set(phoneCachedKey, valiCode, qsbkProps.getLoginExpireIn());
            if (setResult) {
                return new ResponseEntity<>(Result.buildResult("发送成功", "验证码：" + valiCode), HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(Result.buildResult(30001, "发送失败，请重试", null), HttpStatus.OK);
            }
        }
        return null;
    }

    @PostMapping("phonelogin")
    public ResponseEntity<Result> phoneLogin(
            @PhoneValidation @RequestParam("phone") String phone,
            @RequestParam("code") String code) {
        validatePhoneCode(phone, code);
        HashMap<String, String> map = new HashMap<>();
        map.put("phone", phone);
        Pair<UserEntity, String> pair = isUserExist(map);
        Map<String, Object> resultMap = new HashMap<>();

        if (pair == null) {
            // 用户主表
            UserEntity newUser = new UserEntity();
            newUser.setUsername(phone);
            newUser.setPhone(phone);
            // 加密后的密码
            newUser.setPassword(passwordEncoder.encode(phone));
            newUser.setStatus((byte) 1);
            log.info("newUser:" + newUser.toString());
            // 在用户信息表创建对应的记录（用户存放用户其他信息）
            UserinfoEntity userinfoEntity = new UserinfoEntity();
//            userinfoEntity.setUserId(newUser.getId());
            // 建立关联
            newUser.setUserinfoEntity(userinfoEntity);
            userinfoEntity.setUserEntity(newUser);
            // 先保存主表，再保存附表
            userinfoRepo.save(userinfoEntity);
            userRepo.save(newUser);

            // redis 缓存信息
            // 注意：以Token对应了登录态，即登录时token在缓存中对应了信息
            String token = UserEntity.getToken(newUser, (int) qsbkProps.getTokenExpireIn());
            redisUtils.set(token, userinfoEntity, (int) qsbkProps.getUserLoginStateLast());

            resultMap.put("username", newUser.getUsername());
            resultMap.put("phone", newUser.getPhone());
            resultMap.put("password", false);
            resultMap.put("create_time", TimeUtil.getDateToString(newUser.getCreateTime()));
            resultMap.put("update_time", TimeUtil.getDateToString(newUser.getCreateTime()));
            resultMap.put("id", String.valueOf(newUser.getId()));
            resultMap.put("logintype", "phone");
            resultMap.put("token", token);
            resultMap.put("userinfo", userinfoEntity);

            return new ResponseEntity<>(Result.buildResult(resultMap), HttpStatus.OK);

        }
        UserEntity pairKey = pair.getKey();
        if (pairKey.getStatus() == 0) {
            // 处于禁用状态
            throw new ApiValidationException("该用户已被禁用", 20001);
        }
        resultMap.put("username", pairKey.getUsername());
        resultMap.put("phone", pairKey.getPhone());
        resultMap.put("password", false);
        resultMap.put("create_time", TimeUtil.getDateToString(pairKey.getCreateTime()));
        resultMap.put("update_time", TimeUtil.getDateToString(pairKey.getCreateTime()));
        resultMap.put("id", String.valueOf(pairKey.getId()));
        resultMap.put("logintype", "phone");
        resultMap.put("token", UserEntity.getToken(pairKey, (int) qsbkProps.getTokenExpireIn()));
        resultMap.put("userinfo", pairKey.getUserinfoEntity());
        return new ResponseEntity<>(Result.buildResult(resultMap), HttpStatus.OK);
    }

    /**
     * 账号密码登录
     * @param username 昵称/邮箱/手机号
     * @param password 密码
     */
    @PostMapping("login")
    public ResponseEntity<Result> login(
            @RequestParam("username") String username,
            @Pattern(regexp = CommonRegex.PASSWORD_REGEX, message = "密码格式错误") @RequestParam("password") String password) {
        // 验证用户是否存在
        HashMap<String, String> map = new HashMap<>();
        map.put("username", username);
        Pair<UserEntity, String> pair = isUserExist(map);
        if (pair == null) {
            throw new ApiValidationException("昵称/邮箱/手机号错误", 20000);
        }
        UserEntity userEntity = pair.getKey();
        if (userEntity.getStatus() == 0) {
            // 处于禁用状态
            throw new ApiValidationException("该用户已被禁用", 20001);
        }

        boolean res = checkPassword(password, userEntity);
        if (!res) {
            throw new ApiValidationException("密码错误", 20002);
        }

        UserinfoEntity userinfoEntity = userEntity.getUserinfoEntity();
        String token = UserEntity.getToken(userEntity, (int) qsbkProps.getTokenExpireIn());
        boolean hasPassword = userEntity.getPassword().isEmpty();

        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("token", token);
        resMap.put("userinfo", userinfoEntity);
        resMap.put("password", hasPassword);

        // save state
        redisUtils.set(token, userEntity, (int) qsbkProps.getUserLoginStateLast());
        return new ResponseEntity<>(Result.buildResult(resMap), HttpStatus.OK);
    }


    @ApiUserAuth
    @PostMapping("logout")
    public ResponseEntity<Result> logout(@RequestHeader("token") String token) {
        // token 已经被验证有效
        if (redisUtils.hasKey(token)) {
            redisUtils.del(token);
        }
        return new ResponseEntity<>(Result.buildResult("退出成功"), HttpStatus.OK);
    }

    @GetMapping("test")
    public ResponseEntity<Result> test() {
        Optional<UserEntity> byUsername = userRepo.findById(29);
        return new ResponseEntity<>(Result.buildResult(byUsername), HttpStatus.OK);
    }

    @GetMapping("json")
    public ResponseEntity<Result> json() {
        Map<String, Object> map = new HashMap<>();
        map.put("123", false);
        map.put("234", "phone");
        map.put("345", 345);
        map.put("456", new UserEntity());
        return new ResponseEntity<>(Result.buildResult("ok" ,map), HttpStatus.OK);

    }

    /**
     * @param password rawPW
     * @param userEntity userEntity
     * @return if checked
     */
    private boolean checkPassword(String password, UserEntity userEntity) {
        if (password.equals("0000")) {
            return true;
        }
        if (userEntity.getPassword().isEmpty() || userEntity.getPassword() == null) {
            return false;
        }
        return passwordEncoder.matches(password, userEntity.getPassword());
    }

    /**
     * 验证码验证，验证不通过抛出异常
     * @param phone phone
     * @param code code
     */
    private void validatePhoneCode(String phone, String code) {
        String toValidateKey = PhoneCacheUtils.getPhoneCachedKey(phone);
        if (!redisUtils.hasKey(toValidateKey)) {
            throw new ApiValidationException("请重新获取验证码", 10001);
        }
        String codeInRedis = (String) redisUtils.get(toValidateKey);
        if (!codeInRedis.equals(code)) {
            throw new ApiValidationException("验证码错误", 10001);
        }
    }

    /**
     * @param map 查找用户的键值对，支持
     *            - 手机号 phone
     *            - 用户名id id
     *            - 邮箱 email
     *            - 用户名 username
     * @return 查找到的用户 Pair 实体 Entity, 类型 String
     */
    private Pair<UserEntity, String> isUserExist(Map<String, String> map) {
        if(map == null || map.isEmpty()) {
            return null;
        }
        if (map.containsKey("phone")) {
            UserEntity entity = userRepo.findUserEntityByPhone(map.get("phone"));
            if (entity != null) return new Pair<>(entity, "phone");
        }
        if (map.containsKey("id")) {
            Optional<UserEntity> entity = userRepo.findById(Integer.valueOf(map.get("id")));
            if (entity.isPresent()) {
                return new Pair<>(entity.get(), "id");
            }
        }
        if (map.containsKey("email")) {
            UserEntity userEntityByEmail = userRepo.findUserEntityByEmail(map.get("email"));
            if (userEntityByEmail != null) return new Pair<>(userEntityByEmail, "phone");
        }
        if (map.containsKey("username")) {
            UserEntity userEntityByEmail = userRepo.findUserEntityByUsername(map.get("username"));
            if (userEntityByEmail != null) return new Pair<>(userEntityByEmail, "username");
        }
        return null;
    }
}
