package com.rafaelbermudez.encuestas.Entities;

public class Poll {
    private Integer id;
    private Integer uploaded;
    private String firstname;
    private String lastname;
    private Integer age;
    private String answer1;
    private String answer2;
    private String answer3;

    public String getAnswer2() {
        return answer2;
    }

    public void setAnswer2(String answer2) {
        this.answer2 = answer2;
    }

    public String getAnswer3() {
        return answer3;
    }

    public void setAnswer3(String answer3) {
        this.answer3 = answer3;
    }

    public Poll(Integer id, Integer uploaded, String firstname, String lastname, Integer age, String answer1, String answer2, String answer3) {
        this.id = id;
        this.uploaded = uploaded;
        this.firstname = firstname;
        this.lastname = lastname;
        this.age = age;
        this.answer1 = answer1;
        this.answer2 = answer2;
        this.answer3 = answer3;

    }

    public Integer getUploaded() {
        return uploaded;
    }

    public void setUploaded(Integer uploaded) {
        this.uploaded = uploaded;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAnswer1() {
        return answer1;
    }

    public void setAnswer1(String answer1) {
        this.answer1 = answer1;
    }
}
