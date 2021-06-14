package com.example.myapplication;

//테이블이라고 생각하고, 테이블에 들어갈 속성값을 넣기
//파이어베이스는 RDBMS와 다르기 때문에 테이블이라는 개념이 없음. 원래는 키값이라고 부름
public class review {
    String title; //제목
    String content; //내용

    public review(){}



    public String gettitle() { return title;
    }

    public void settitle(String name) {
        this.title = name;
    }

    public String getcontent() {
        return content;
    }

    public void setcontent(String content) {
        this.content = content;
    }




    //값을 추가할때 쓰는 함수, MainActivity에서 addreview함수에서 사용할 것임.
    public review(String name, String content){
        this.title = name;
        this.content = content;
    }
}