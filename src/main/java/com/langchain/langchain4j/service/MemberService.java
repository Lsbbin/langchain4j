package com.langchain.langchain4j.service;

import com.langchain.langchain4j.mapper.MemberMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MemberService {

    @Autowired
    private MemberMapper memberMapper;

    public List<Map> list () {
        return memberMapper.list();
    }
}
