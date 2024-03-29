package fun.aaronfang.qsbk.demo.model;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "user", schema = "info")
public class UserEntity {
    private int id;
    private String username;
    private String userpic;
    private String password;
    private String phone;
    private String email;
    private byte status;
    private Integer createTime;
    private UserinfoEntity userinfoEntity;
    private List<PostEntity> postEntityList;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Basic
    @Column(name = "userpic")
    public String getUserpic() {
        return userpic;
    }

    public void setUserpic(String userpic) {
        this.userpic = userpic;
    }

    @Basic
    @Column(name = "password")
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Basic
    @Column(name = "phone")
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Basic
    @Column(name = "email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Basic
    @Column(name = "status")
    public byte getStatus() {
        return status;
    }

    public void setStatus(byte status) {
        this.status = status;
    }

    @Basic
    @Column(name = "create_time")
    public Integer getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Integer createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return id == that.id && status == that.status && Objects.equals(username, that.username) && Objects.equals(userpic, that.userpic) && Objects.equals(password, that.password) && Objects.equals(phone, that.phone) && Objects.equals(email, that.email) && Objects.equals(createTime, that.createTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, userpic, password, phone, email, status, createTime);
    }

    @PrePersist
    void createAt() {
        this.createTime = Math.toIntExact(System.currentTimeMillis() / 1000);
    }

    @OneToOne(mappedBy = "userEntity")
    @JsonIgnore
    public UserinfoEntity getUserinfoEntity() {
        return userinfoEntity;
    }

    public void setUserinfoEntity(UserinfoEntity userinfoEntity) {
        this.userinfoEntity = userinfoEntity;
    }

    @OneToMany(mappedBy = "userEntityWithPost", cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    @JsonIgnore
    public List<PostEntity> getPostEntityList() {
        return postEntityList;
    }

    public void setPostEntityList(List<PostEntity> postEntityList) {
        this.postEntityList = postEntityList;
    }

    /**
     * 根据User信息生成Token
     * @param user UserEntity实体
     * @return token
     */
    public static String getToken(UserEntity user, int expireIn) {
        String token;
        token= JWT.create()
                .withAudience(String.valueOf(user.getId()))
                .withExpiresAt(new Date(System.currentTimeMillis() + expireIn))
                .withIssuedAt(new Date(System.currentTimeMillis()))
                .sign(Algorithm.HMAC256(user.getPassword()));
        return token;
    }
}
