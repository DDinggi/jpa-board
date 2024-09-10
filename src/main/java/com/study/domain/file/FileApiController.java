package com.study.domain.file;

import com.study.common.file.FileUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class FileApiController {

    private final FileService fileService;
    private final FileUtils fileUtils;
    private static final Logger logger = LoggerFactory.getLogger(FileApiController.class);

    // 파일 리스트 조회
    @GetMapping("/posts/{postId}/files")
    public List<FileResponse> findAllFileByPostId(@PathVariable("postId") Long postId) {
        logger.info("파일 리스트 조회 요청 - 게시글 ID: {}", postId);
        try {
            List<FileResponse> files = fileService.findAllFileByPostId(postId);
            logger.info("파일 리스트 조회 결과 - 조회된 파일 수: {}", files.size());
            return files;
        } catch (Exception e) {
            logger.error("파일 리스트 조회 중 오류 발생: 게시글 ID: {}", postId, e);
            throw new RuntimeException("파일 리스트 조회 중 오류 발생", e);
        }
    }

    // 첨부파일 다운로드
    @GetMapping("/posts/{postId}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable("postId") Long postId, @PathVariable("fileId") Long fileId) {
        logger.info("파일 다운로드 요청 - 게시글 ID: {}, 파일 ID: {}", postId, fileId);

        FileResponse file = null;  // file 변수를 try 블록 바깥에서 선언합니다.

        try {
            file = fileService.findFileById(fileId);
            if (file == null) {
                logger.warn("파일을 찾을 수 없습니다. 파일 ID: {}", fileId);
                return ResponseEntity.notFound().build();
            }

            // 파일을 리소스로 읽어오기
            Resource resource = fileUtils.readFileAsResource(file);
            if (resource == null) {
                logger.warn("리소스를 찾을 수 없습니다. 파일 ID: {}, 파일 경로: {}", fileId, file.getSaveName());
                return ResponseEntity.notFound().build();
            }

            // 파일명 인코딩
            String filename = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            logger.info("파일 다운로드 준비 완료 - 파일명: {}", filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\";")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getSize()))
                    .body(resource);

        } catch (UnsupportedEncodingException e) {
            logger.error("파일명 인코딩 실패: " + (file != null ? file.getOriginalName() : "알 수 없음"), e);
            throw new RuntimeException("filename encoding failed: " + (file != null ? file.getOriginalName() : "알 수 없음"), e);
        } catch (Exception e) {
            logger.error("파일 다운로드 중 오류 발생 - 게시글 ID: {}, 파일 ID: {}", postId, fileId, e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
