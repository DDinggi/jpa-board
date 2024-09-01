package com.study.domain.post;

import com.study.common.dto.MessageDto;
import com.study.common.dto.SearchDto;
import com.study.common.paging.PagingResponse;
import com.study.domain.file.FileService;  // FileService import
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final FileService fileService; // 파일 저장을 위한 서비스 추가

    // 게시글 작성 페이지
    @GetMapping("/post/write.do")
    public String openPostWrite(@RequestParam(value = "id", required = false) Long id,
                                @RequestParam(value = "type", defaultValue = "1") Integer type,
                                Model model) {
        if (id != null) {
            PostResponse post = postService.findPostById(id);
            if (post != null) {
                model.addAttribute("post", post);
            }
        }
        model.addAttribute("boardType", type); // boardType 값을 모델에 설정
        return "post/write";
    }

    @GetMapping("/post/view.do")
    public String openPostView(@RequestParam("id") Long id,
                               @RequestParam(value = "type", defaultValue = "1") Integer type,
                               Model model) {
        PostResponse post = postService.findPostById(id);

        if (post == null) {
            return "redirect:/post/list.do?type=" + type; // 게시글이 없을 경우 리스트로 리다이렉트
        }

        model.addAttribute("post", post);
        model.addAttribute("boardType", type);
        return "post/view";
    }

    // 게시글 리스트 페이지
    @GetMapping("/post/list.do")
    public String openPostList(@ModelAttribute("params") final SearchDto params,
                               @RequestParam(value = "type", defaultValue = "1") Integer type, Model model) {
        params.setType(type);
        PagingResponse<PostResponse> response = postService.findAllPost(params);
        model.addAttribute("response", response);
        model.addAttribute("boardType", type);

        return "post/list";
    }

    @PostMapping("/post/save.do")
    @ResponseBody
    public Map<String, Object> savePost(@ModelAttribute PostRequest params,
                                        @RequestParam("type") Integer type,
                                        @RequestParam("files") List<MultipartFile> files) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 게시글 저장 로직...
            Long id = postService.savePost(params);
            saveFiles(id, files); // 파일 저장 로직

            response.put("success", true);
            response.put("message", "저장이 완료되었습니다.");
            response.put("id", id); // 생성된 게시글 id 반환
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "저장 중 오류가 발생했습니다: " + e.getMessage());
        }
        return response;
    }


    @PostMapping("/post/update.do")
    @ResponseBody
    public Map<String, Object> updatePost(@ModelAttribute PostRequest params,
                                          @RequestParam("type") Integer type,
                                          @RequestParam("files") List<MultipartFile> files,
                                          @RequestParam(value = "removeFileIds", required = false) List<Long> removeFileIds) {
        Map<String, Object> response = new HashMap<>();
        try {
            params.setType(type);  // type 값을 설정

            // 게시글 업데이트 로직
            postService.updatePost(params);

            // 삭제할 파일이 있는 경우 파일 삭제 로직 수행
            if (removeFileIds != null && !removeFileIds.isEmpty()) {
                fileService.deleteAllFileByIds(removeFileIds);
            }

            // 파일 저장 로직
            saveFiles(params.getId(), files);

            // 성공 응답 설정
            response.put("success", true);
            response.put("message", "수정이 완료되었습니다.");
        } catch (Exception e) {
            // 오류 발생 시 예외 메시지를 반환
            response.put("success", false);
            response.put("message", "수정 중 오류가 발생했습니다: " + e.getMessage());
        }
        return response;
    }


    // 게시글 삭제 메서드 추가
    @PostMapping("/post/delete.do")
    @ResponseBody
    public Map<String, Object> deletePost(@RequestParam("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            postService.deletePost(id);
            response.put("success", true);
            response.put("message", "삭제가 완료되었습니다.");
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return response;
    }

    // 파일 저장 로직을 별도의 메서드로 추출
    private void saveFiles(Long postId, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                fileService.saveFile(postId, file); // 파일 저장 로직
                System.out.println("파일이 저장되었습니다: " + file.getOriginalFilename());
            }
        }
    }
}
