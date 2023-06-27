package com.kh.backend_finalproject.service;

import com.kh.backend_finalproject.constant.IsActive;
import com.kh.backend_finalproject.constant.IsMembership;
import com.kh.backend_finalproject.constant.IsPush;
import com.kh.backend_finalproject.dto.*;
import com.kh.backend_finalproject.entitiy.*;
import com.kh.backend_finalproject.dto.UserProfileDto;
import com.kh.backend_finalproject.repository.FolderRepository;
import com.kh.backend_finalproject.repository.PostRepository;
import com.kh.backend_finalproject.repository.ReplyRepository;
import com.kh.backend_finalproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.mail.Folder;
import javax.servlet.http.HttpServletRequest;
import javax.swing.text.html.Option;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final FolderRepository folderRepository;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;



    // 🔐 마이페이지 - 회원 프로필 바 가져오기 (프로필사진, 닉네임, 멤버십 여부, 한 줄 소개, 총 게시글/댓글 수)
    public UserDto getUserProfileInfo(HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);
        Optional<UserTb> user = userRepository.findById(authUser.getId());

        UserDto userProfileDto = new UserDto();
        userProfileDto.setNickname(user.get().getNickname());
        userProfileDto.setUserComment(user.get().getUserComment());
        userProfileDto.setPfImg(user.get().getPfImg());
        userProfileDto.setIsMembership(user.get().getIsMembership());
        userProfileDto.setPostCount(user.get().getPosts().size());
        userProfileDto.setReplyCount(user.get().getReplies().size());

        return userProfileDto;
    }

    // 🔐 마이페이지 - 회원의 모든 게시글 가져오기 (글 번호, 제목, 본문, 조회수, 작성일, 작성자 닉네임)
    public List<UserDto> getAllUserPosts(HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);
        Optional<UserTb> user = userRepository.findById(authUser.getId());
        List<PostTb> posts = user.get().getPosts();

        List<UserDto> userDtoList = new ArrayList<>();
        for (PostTb post : posts) {
            UserDto userDto = new UserDto();
            userDto.setPostNum(post.getId());
            userDto.setTitle(post.getTitle());
            userDto.setContent(post.getContent());
            userDto.setViewCount(post.getViewCount());
            userDto.setWriteDate(post.getWriteDate());
            userDto.setNickname(post.getUser().getNickname());
            userDtoList.add(userDto);
        }
        return userDtoList;
    }

    // 🔐 마이페이지 - 회원의 게시글 삭제하기
    public boolean deletePosts(List<Long> postIds, HttpServletRequest request,
                               UserDetails userDetails) throws IllegalAccessException {

        UserTb user = authService.validateTokenAndGetUser(request, userDetails);

        for (Long postId : postIds) {
            Optional<PostTb> postOptional = postRepository.findById(postId);
            if (postOptional.isPresent()) {
                PostTb post = postOptional.get();
                postRepository.delete(post);
            } else {
                throw new IllegalArgumentException("해당 게시글이 존재하지 않습니다.");
            }
        }
        return true;
    }

    // 🔐 마이페이지 - 회원의 모든 댓글 가져오기 (댓글 번호, 작성자 닉네임, 댓글 본문, 원문 제목, 작성일, 원문 번호)
    public List<UserDto> getAllUserReplies(HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);
        Optional<UserTb> user = userRepository.findById(authUser.getId());

        List<ReplyTb> replies = user.get().getReplies();

        List<UserDto> userDtoList = new ArrayList<>();
        for (ReplyTb reply : replies) {
            UserDto userDto = new UserDto();
            userDto.setReplyNum(reply.getId());
            userDto.setNickname(reply.getUser().getNickname());
            userDto.setContent(reply.getContent());
            userDto.setTitle(reply.getPost().getTitle());
            userDto.setWriteDate(reply.getWriteDate());
            userDto.setPostNum(reply.getPost().getId());
            userDtoList.add(userDto);
        }
        return userDtoList;
    }

    // 🔐 마이페이지 - 회원의 댓글 삭제하기
    public boolean deleteReplies(List<Long> replyIds, HttpServletRequest request,
                                 UserDetails userDetails) throws IllegalAccessException {

        UserTb user = authService.validateTokenAndGetUser(request, userDetails);

        for (Long replyId : replyIds) {
            Optional<ReplyTb> replyOptionl = replyRepository.findById(replyId);
            if (replyOptionl.isPresent()) {
                ReplyTb reply = replyOptionl.get();
                replyRepository.delete(reply);
            } else {
                throw new IllegalArgumentException("해당 댓글이 존재하지 않습니다.");
            }
        }
        return true;
    }

    // 🔐 마이페이지 - 회원의 멤버십 상태 조회
    public IsMembership getUserMembershipStatus(HttpServletRequest request, UserDetails userDetails) {

        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        Optional<UserTb> user = userRepository.findById(authUser.getId());
        return user.get().getIsMembership();
    }

    // 🔐 마이페이지 - 회원의 푸쉬알림 상태 조회
    public IsPush getUserNotificationStatus(HttpServletRequest request, UserDetails userDetails) {

        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);
        Optional<UserTb> user = userRepository.findById(authUser.getId());
        return user.get().getIsPush();
    }

    // 🔐 마이페이지 - 회원의 푸쉬알림 상태 변경
    public IsPush updateUserNotificationStatus(HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        Optional<UserTb> user = userRepository.findById(authUser.getId());
        IsPush currentStatus = user.get().getIsPush();
        System.out.println("🍒(" + user + ")현재 알림 설정 상태  : " + currentStatus);

        IsPush newStatus = currentStatus.equals(IsPush.PUSH) ? IsPush.NOPUSH : IsPush.PUSH;
        user.get().setIsPush(newStatus);
        userRepository.save(user.get());
        System.out.println("🍒(" + user + ")변경된 알림 설정 : " + newStatus);

        return newStatus;
    }

    // 🔐 마이페이지 - 회원의 북마크 폴더 생성하기
    public boolean createBookmarkFolder(FolderDto folderDto, HttpServletRequest request, UserDetails userDetails) throws IllegalAccessException {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        if (authUser != null) {
            FolderTb folderTb = new FolderTb();
            folderTb.setName(folderDto.getName());
            folderTb.setUser(authUser);

            FolderTb savedFolder = folderRepository.save(folderTb);
            return savedFolder != null;
        } else {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
    }

    // 🔐 마이페이지 - 회원의 북마크 폴더 삭제하기
    public boolean deleteBookmarkFolder(Long folderId, HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        Optional<FolderTb> folderOptional = folderRepository.findById(folderId);
        if (folderOptional.isPresent()) {
            FolderTb folder = folderOptional.get();
            folderRepository.delete(folder);
            return true;
        } else {
            throw new IllegalArgumentException("해당 폴더가 존재하지 않습니다.");
        }
    }

    // 🔐 마이페이지 - 회원의 북마크 폴더 이름 변경하기
    public boolean updateBookmarkFolderName(Long folderId, String folderName, HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        Optional<FolderTb> folderOptional = folderRepository.findById(folderId);
        if (folderOptional.isPresent()) {
            FolderTb folder = folderOptional.get();

            if (folder.getUser().getId().equals(authUser.getId())) {
                folder.setName(folderName);
                folderRepository.save(folder);
                return true;
            } else {
                throw new IllegalArgumentException("해당 폴더에 접근 권한이 없습니다.");
            }
        } else {
            throw new IllegalArgumentException("해당 폴더가 존재하지 않습니다.");
        }
    }

    // 🔐 마이페이지 - 회원의 북마크 폴더 가져오기
    public List<FolderDto> getUserBookmarkFolders(HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        Optional<UserTb> user = userRepository.findById(authUser.getId());
        if (user != null) {
            List<FolderDto> folderDtos = new ArrayList<>();
            for (FolderTb folder : user.get().getFolders()) {
                FolderDto folderDto = new FolderDto();
                folderDto.setId(folder.getId());
                folderDto.setName(folder.getName());

                List<BookmarkDto> bookmarkDtos = new ArrayList<>();
                for (BookmarkTb bookmark : folder.getBookmarks()) {
                    BookmarkDto bookmarkDto = new BookmarkDto();
                    bookmarkDto.setId(bookmark.getId());
                    bookmarkDto.setPostId(bookmark.getPost().getId());
                    bookmarkDto.setImgUrl(bookmark.getPost().getImgUrl());

                    bookmarkDtos.add(bookmarkDto);
                }
                folderDto.setBookmarks(bookmarkDtos);

                folderDtos.add(folderDto);
            }
            return folderDtos;
        }
        return Collections.emptyList();
    }

    // 🔐 마이페이지 - 회원의 북마크 가져오기
    public List<BookmarkDto> getBookmarksInFolder(Long folderId, HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        Optional<FolderTb> folderOptional = folderRepository.findById(folderId);
        if (folderOptional.isPresent()) {
            FolderTb folder = folderOptional.get();
            // 폴더 소유자 확인
            if (folder.getUser().getId().equals(authUser.getId())) {
                List<BookmarkDto> bookmarkDtos = new ArrayList<>();
                for (BookmarkTb bookmark : folder.getBookmarks()) {
                    BookmarkDto bookmarkDto = new BookmarkDto();
                    bookmarkDto.setId(bookmark.getId());
                    bookmarkDto.setPostId(bookmark.getPost().getId());
                    bookmarkDto.setImgUrl(bookmark.getPost().getImgUrl());
                    bookmarkDto.setTitle(bookmark.getPost().getTitle());
                    bookmarkDto.setDistrict(bookmark.getPost().getDistrict());

                    bookmarkDtos.add(bookmarkDto);
                }
                return bookmarkDtos;
            } else {
                throw new IllegalArgumentException("해당 폴더에 접근 권한이 없습니다.");
            }
        }
        return Collections.emptyList();
    }

    // 🔐 마이페이지 - 회원의 북마크 폴더 이름 가져오기
    public String getFolderName(Long folderId, HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);
        Optional<FolderTb> user = folderRepository.findById(folderId);
        String folderName = user.get().getName();
        return folderName;
    }

    // 🔐 마이페이지 - 회원정보 가져오기
    public UserDto getUserInfo(HttpServletRequest request, UserDetails userDetails) {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);
        Optional<UserTb> user = userRepository.findById(authUser.getId());
        UserDto userdto = new UserDto();
        userdto.setId(user.get().getId());
        userdto.setPfImg(user.get().getPfImg());
        userdto.setNickname(user.get().getNickname());
        userdto.setEmail(user.get().getEmail());
        userdto.setUserComment(user.get().getUserComment());
        userdto.setUserRegion(user.get().getUserRegion());
        return userdto;
    }

    // 🔐 마이페이지 - 회원정보 수정
    public boolean updateInformation(UserDto userDto, HttpServletRequest request, UserDetails userDetails) throws IllegalAccessException {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        UserTb user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new IllegalAccessException("해당 회원이 없습니다."));

//        if (userDto.getNickname() == null || userDto.getNickname().isEmpty()
//                || userDto.getUserComment() == null || userDto.getUserComment().isEmpty()) {
//            throw new IllegalArgumentException("모든 정보를 입력해 주세요..😰");
//        }

        user.setPfImg(userDto.getPfImg());
        user.setNickname(userDto.getNickname());
        user.setUserComment(userDto.getUserComment());
        user.setUserRegion(userDto.getUserRegion());
        userRepository.save(user);

        return true;
    }

    // 🔐 마이페이지 - 비밀번호 변경
    public boolean updatePwd(UserTb userTb, HttpServletRequest request, UserDetails userDetails) throws IllegalAccessException {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        UserTb user = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));

        if (userTb.getPwd() == null || userTb.getPwd().isEmpty()) {
            throw new IllegalArgumentException("비밀번호가 없어요..😰");
        }

        String encodedPassword = passwordEncoder.encode(userTb.getPwd());
        user.setPwd(encodedPassword);
        UserTb savedUser = userRepository.save(user);
        log.info(savedUser.toString());

        return true;
    }

    // 🔐 마이페이지 - 회원 탈퇴
    public void deleteUser(HttpServletRequest request, UserDetails userDetails) throws IllegalAccessException {
        UserTb authUser = authService.validateTokenAndGetUser(request, userDetails);

        UserTb userTb = userRepository.findById(authUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 회원이 없습니다."));
        userRepository.delete(userTb);
    }

    // ✅ 회원가입 - 닉네임 중복 확인
    public Optional<UserTb> findUserByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    // ✅ 회원가입 - 이메일 인증 (인증키 생성 + 메일 전송)
    public boolean findUserByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // ✅ 회원가입 - 이메일 인증 (인증키 확인)
    public void checkEmailWithAuthKey(String email, String authKey) throws IllegalArgumentException {
        Optional<UserTb> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            UserTb user = userOptional.get();
            if (authKey.equals(user.getAuthKey())) {
                user.setIsActive(IsActive.ACTIVE);
                user.setAuthKey("");
                userRepository.save(user);
                System.out.println("🍒 이메일 인증 완료: " + email);
            } else {
                throw new IllegalArgumentException("인증키가 올바르지 않습니다.");
            }
        } else {
            throw new IllegalArgumentException("이메일 주소를 찾을 수 없습니다.: " + email);
        }
    }
}
