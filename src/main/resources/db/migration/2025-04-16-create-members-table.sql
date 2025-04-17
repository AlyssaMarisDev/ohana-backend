create database ohana;
use ohana;

create table members (
    id int primary key auto_increment,
    name varchar(255) not null,
    age int,
    gender varchar(255),
    email varchar(255) not null unique,
    password varchar(255) not null
);
