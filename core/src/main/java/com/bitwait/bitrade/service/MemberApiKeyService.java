package com.bitwait.bitrade.service;

import com.bitwait.bitrade.service.Base.BaseService;
import com.bitwait.bitrade.dao.MemberApiKeyDao;
import com.bitwait.bitrade.entity.MemberApiKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description: MemberApiKeyService
 * @author ragan QQ:2098401701 E-mail:bitwait@qq.com
 * @create: 2019/05/07 10:40
 */
@Service
public class MemberApiKeyService extends BaseService<MemberApiKey> {

    @Autowired
    private MemberApiKeyDao apiKeyDao ;

    public MemberApiKey findMemberApiKeyByApiKey(String apiKey) {
        return apiKeyDao.findMemberApiKeyByApiKey(apiKey);
    }

    public MemberApiKey findByMemberIdAndId(Long memberId,Long id){
        return apiKeyDao.findMemberApiKeyByMemberIdAndId(memberId,id);
    }

    public MemberApiKey save(MemberApiKey memberApiKey){
        return apiKeyDao.save(memberApiKey);
    }


    public void del(Long id){
        apiKeyDao.del(id);
    }

    public List<MemberApiKey> findAllByMemberId(Long memberId){
        return apiKeyDao.findAllByMemberId(memberId);
    }
}
