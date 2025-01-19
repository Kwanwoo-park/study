create table board (
    id bigint auto_increment primary key,
    title varchar(200) not null,
    content text not null,
    read_cnt int not null default 0,
    register_name varchar(100) not null,
    register_email varchar(200) not null,
    register_time datetime null default null,
    update_time datetime null default null
);

create table member (
    id bigint auto_increment primary key,
    email varchar(200) not null,
    pwd varchar(200) not null,
    name varchar(100) not null,
    role varchar(20) not null,
    last_login_time datetime null default null,
    register_time datetime null default null,
    update_time datetime null default null,
    profile varchar(200) not null default KakaoTalk_Photo_2023-04-14-21-36-15.jpeg);
create unique index email on member (email);

create table book (
    id bigint auto_increment primary key,
    bnum varchar(20) not null,
    title varchar(20) not null,
    author varchar(10) not null,
    publisher varchar(20) not null,
    bsort varchar(10) not null,
    ssort varchar(10) not null,
    cond int default 0,
    borw int default 0
);

create table book_borrow (
    id bigint auto_increment primary key,
    bnum varchar(20) not null,
    title varchar(20) not null,
    mid bigint not null,
    name varchar(100) not null,
    email varchar(200) not null
);

create table comment (
    id bigint auto_increment primary key,
    comments text not null,
    mid bigint not null,
    mname varchar(100) not null,
    email varchar(200) not null,
    bid bigint not null,
    register_time datetime null default null,
    update_time datetime null default null
);

create table follow (
    id bigint auto_increment primary key,
    follower bigint not null,
    follower_name varchar(100) not null,
    follower_email varchar(200) not null,
    following bigint not null,
    following_name varchar(100) not null,
    following_email varchar(200) not null
);

create table room (
    id bigint auto_increment primary key,
    room_id varchar(200) not null,
    name varchar(100) not null,
    count bigint not null
);

create table message (
    id bigint auto_increment primary key,
    room_id varchar(200) not null,
    sender varchar(100) not null,
    email varchar(200) not null,
    message varchar(200) not null,
    type varchar(20) not null,
    register_time datetime null default null,
    update_time datetime null default null
);

create table room_member (
    id bigint auto_increment primary key,
    room_id varchar(200) not null,
    mem_name varchar(100) not null,
    email varchar(200) not null
);