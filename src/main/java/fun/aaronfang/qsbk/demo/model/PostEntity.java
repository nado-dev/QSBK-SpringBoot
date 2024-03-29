package fun.aaronfang.qsbk.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "post", schema = "info")
public class PostEntity {
    private int id;
    private String title;
    private String titlepic;
    private String content;
    private int sharenum;
    private String path;
    private byte type;
    private Integer createTime;
    private Integer postClassId;
    private Integer shareId;
    private Byte isopen;
    private List<TopicEntity> topicEntityList;
    private UserEntity userEntityWithPost;
    private List<ImageEntity> imageEntityList;

    @Id
    @Column(name = "id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Basic
    @Column(name = "title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Basic
    @Column(name = "titlepic")
    public String getTitlepic() {
        return titlepic;
    }

    public void setTitlepic(String titlepic) {
        this.titlepic = titlepic;
    }

    @Basic
    @Column(name = "content")
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Basic
    @Column(name = "sharenum")
    public int getSharenum() {
        return sharenum;
    }

    public void setSharenum(int sharenum) {
        this.sharenum = sharenum;
    }

    @Basic
    @Column(name = "path")
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Basic
    @Column(name = "type")
    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    @Basic
    @Column(name = "create_time")
    public Integer getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Integer createTime) {
        this.createTime = createTime;
    }

    @Basic
    @Column(name = "post_class_id")
    public Integer getPostClassId() {
        return postClassId;
    }

    public void setPostClassId(Integer postClassId) {
        this.postClassId = postClassId;
    }

    @Basic
    @Column(name = "share_id")
    public Integer getShareId() {
        return shareId;
    }

    public void setShareId(Integer shareId) {
        this.shareId = shareId;
    }

    @Basic
    @Column(name = "isopen")
    public Byte getIsopen() {
        return isopen;
    }

    public void setIsopen(Byte isopen) {
        this.isopen = isopen;
    }

    @ManyToMany(mappedBy = "postEntityList")
    @JsonIgnore
    public List<TopicEntity> getTopicEntityList() {
        return topicEntityList;
    }

    public void setTopicEntityList(List<TopicEntity> topicEntityList) {
        this.topicEntityList = topicEntityList;
    }

    @ManyToOne(cascade = CascadeType.ALL, targetEntity = UserEntity.class)
    @JoinColumn(name = "user_id", referencedColumnName="id")
    @JsonIgnore
    public UserEntity getUserEntityWithPost() {
        return userEntityWithPost;
    }

    public void setUserEntityWithPost(UserEntity userEntity) {
        this.userEntityWithPost = userEntity;
    }


    @ManyToMany(mappedBy = "postEntityList")
    @JsonProperty("images")
    public List<ImageEntity> getImageEntityList() {
        return imageEntityList;
    }

    public void setImageEntityList(List<ImageEntity> imageEntityList) {
        this.imageEntityList = imageEntityList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PostEntity that = (PostEntity) o;
        return id == that.id && sharenum == that.sharenum && type == that.type && Objects.equals(title, that.title) && Objects.equals(titlepic, that.titlepic) && Objects.equals(content, that.content) && Objects.equals(path, that.path) && Objects.equals(createTime, that.createTime) && Objects.equals(postClassId, that.postClassId) && Objects.equals(shareId, that.shareId) && Objects.equals(isopen, that.isopen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, titlepic, content, sharenum, path, type, createTime, postClassId, shareId, isopen);
    }

    @PrePersist
    void createAt() {
        this.createTime = Math.toIntExact(System.currentTimeMillis() / 1000);
    }
}
