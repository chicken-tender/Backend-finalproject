package com.kh.backend_finalproject.service;
import com.kh.backend_finalproject.dto.*;
import com.kh.backend_finalproject.entitiy.*;
import com.kh.backend_finalproject.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AdminService {
    private final ChatbotRepository chatbotRepository ;
    private final UserRepository userRepository;
    private  final ReplyRepository replyRepository;
    private  final AdRepository adRepository;
    private final PostRepository postRepository;
    private final ReportRepository reportRepository;

    // 💗 전체 회원 조회
    public List<UserDto> findAllUserList() {
        List<UserTb> users = userRepository.findAll();
        List<UserDto> userDtos = new ArrayList<>();
        for (UserTb user : users) {
            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setNickname(user.getNickname());
            userDto.setEmail(user.getEmail());
            userDto.setIsMembership(user.getIsMembership());
            userDto.setRegDate(user.getRegDate());

            List<String> blockedNickname = new ArrayList<>();
            List<BlockTb> blockedUsers = user.getBlockedUsers();
            for (BlockTb block : blockedUsers) {
                blockedNickname.add(block.getBlocked().getNickname());
            }
            userDto.setBlockedNickname(blockedNickname);
            userDtos.add(userDto);
        }
        return userDtos;
    }

    // 💗 전체 문의 내역 조회 (문의일 최근순 정렬)
    public List<ChatbotUserDto> findAllInquiryList() {
        List<ChatbotUserDto> chatbotUserDtos = chatbotRepository.findAllInquiryWithUserNickname();
        return chatbotUserDtos;
    }

    // 💗 전체 게시글 내역 조회 (문의일 최근순 정렬)
    public List<PostUserDto> findAllPostList() {
        List<PostUserDto> postUserDtos = postRepository.findAllPostsWithUserNickname();
        return postUserDtos;
    }

    // 💗 전체 댓글 내역 조회 (문의일 최근순 정렬)
    public List<ReplyUserDto> findAllReplyList() {
        List<ReplyUserDto> replyUserDtos = replyRepository.findAllReplyWithUserNickname();
        return replyUserDtos;
    }

    // 💗 전체 광고 내역 조회
    public List<AdDto> findAllAdList() {
        List<AdTb> ads = adRepository.findAll();
        List<AdDto> adDtos = new ArrayList<>();
        for (AdTb e : ads) {
            AdDto adDto = new AdDto();
            adDto.setAdNum(e.getId());
            adDto.setName(e.getName());
            adDto.setImgUrl(e.getImgUrl());
            adDtos.add(adDto);
        }
        return adDtos;
    }

    // 💗 광고 추가
    public AdDto createAd(AdDto adDto) {
        AdTb adTb = new AdTb();
        adTb.setName(adDto.getName());
        adTb.setImgUrl(adDto.getImgUrl());
        adRepository.save(adTb);

        AdDto savedAdDto = new AdDto();
        savedAdDto.setAdNum(adTb.getId());
        savedAdDto.setName(adTb.getName());
        savedAdDto.setImgUrl(adTb.getImgUrl());
        return savedAdDto;
    }

    // 💗 전체 신고 내역 조회
    public List<ReportDto> findAllReportList() {
        List<ReportTb> reports = reportRepository.findAll();
        List<ReportDto> reportDtos = new ArrayList<>();
        for (ReportTb report : reports) {
            ReportDto reportDto = new ReportDto();
            reportDto.setReportNum(report.getId());
            reportDto.setContent(report.getContent());
            reportDto.setReporter(report.getReporter().getNickname());
            reportDto.setReportDate(report.getReportDate());
            reportDtos.add(reportDto);
        }
        return reportDtos;
    }

    // 💗다중 회원 삭제
    public void deleteUsers(List<Long> userIds) {
        for (Long userId : userIds) {
            userRepository.deleteById(userId);
        }
    }

    // 💗다중 게시글 삭제
    public void deletePosts(List<Long> postIds) {
        for (Long postId : postIds) {
            postRepository.deleteById(postId);
        }
    }

    // 💗다중 댓글 삭제
    public void deleteReplies(List<Long> replyIds) {
        for (Long replyId : replyIds) {
            replyRepository.deleteById(replyId);
        }
    }

    // 💗다중 광고 삭제
    public void deleteAds(List<Long> adIds) {
        for (Long adId : adIds) {
            adRepository.deleteById(adId);
        }
    }

    //💗 관리자 - 회원 검색
    public List<UserDto> findByKeywordUser(String keyword) {
        List<UserTb> user = userRepository.findByKeywordUser(keyword);
        List<UserDto> userDtos = new ArrayList<>();
        for (UserTb e : user) {
            UserDto dto = new UserDto();
            dto.setId(e.getId());
            dto.setNickname(e.getNickname());
            dto.setEmail(e.getEmail());
            dto.setIsMembership(e.getIsMembership());
            dto.setRegDate(e.getRegDate());

            List<String> blockedNickname = new ArrayList<>();
            List<BlockTb> blockedUsers = e.getBlockedUsers();
            for (BlockTb block : blockedUsers) {
                blockedNickname.add(block.getBlocked().getNickname());
            }
            dto.setBlockedNickname(blockedNickname);
            userDtos.add(dto);
        }
        return userDtos;
    }

    //💗 관리자 - 게시글 검색
    public List<PostUserDto> findByKeywordAdminPost(String keyword) {
        List<PostTb> postList = postRepository.findByKeywordAdminPost(keyword);
        List<PostUserDto> postUserDtos = new ArrayList<>();
        for (PostTb e : postList) {
            PostUserDto dto = new PostUserDto();
            dto.setId(e.getId());
            dto.setTitle(e.getTitle());
            dto.setNickname(e.getUser().getNickname());
            dto.setWriteDate(e.getWriteDate());
            postUserDtos.add(dto);
        }
        return postUserDtos;
    }

    //💗 관리자 - 댓글 검색
    public List<ReplyUserDto> findByKeywordReply(String keyword) {
        List<ReplyTb> replyList = replyRepository.findByKeywordReply(keyword);
        List<ReplyUserDto> replyUserDtos = new ArrayList<>();
        for (ReplyTb e : replyList) {
            ReplyUserDto dto = new ReplyUserDto();
            dto.setReplyNum(e.getId());
            dto.setContent(e.getContent());
            dto.setNickname(e.getUser().getNickname());
            dto.setWriteDate(e.getWriteDate());
            replyUserDtos.add(dto);
        }
        return replyUserDtos;
    }
}

