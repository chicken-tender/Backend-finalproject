package com.kh.backend_finalproject.service;
import com.kh.backend_finalproject.dto.UserDto;
import com.kh.backend_finalproject.dto.UserProfileDto;
import com.kh.backend_finalproject.entitiy.PostTb;
import com.kh.backend_finalproject.entitiy.UserTb;
import com.kh.backend_finalproject.repository.PostRepository;
import com.kh.backend_finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    // ✅ 마이페이지 - 회원 프로필 바 가져오기 (프로필사진, 닉네임, 멤버십 여부, 한 줄 소개, 총 게시글/댓글 수)
    public List<UserProfileDto> getUserProfileInfo(String email) {
        List<UserProfileDto> profileDtos = userRepository.findUserProfileInfo(email);
        return profileDtos;
    }
    // 마이페이지 - 회원 전체 게시글 가져오기
//    public List<PostDto> getUserPosts(Long userNum) {
//        List<PostDto> userPosts = userRepository.findUserPosts(userNum);
//        return userPosts;
//    }
}
