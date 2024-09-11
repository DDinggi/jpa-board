package com.study.common.file;

import com.study.domain.file.FileRequest;
import com.study.domain.file.FileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FileUtils {

    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);
    private final String uploadPath = Paths.get("C:", "develop", "upload-files").toString();

    /**
     * 다중 파일 업로드
     * @param multipartFiles - 파일 객체 List
     * @return DB에 저장할 파일 정보 List
     */
    public List<FileRequest> uploadFiles(final List<MultipartFile> multipartFiles) {
        List<FileRequest> files = new ArrayList<>();
        for (MultipartFile multipartFile : multipartFiles) {
            if (multipartFile.isEmpty()) {
                logger.warn("업로드된 파일이 비어 있습니다. 파일을 건너뜁니다.");
                continue;
            }
            FileRequest fileRequest = uploadFile(multipartFile);
            if (fileRequest != null) {
                files.add(fileRequest);
            }
        }
        return files;
    }

    /**
     * 단일 파일 업로드
     * @param multipartFile - 파일 객체
     * @return DB에 저장할 파일 정보
     */
    public FileRequest uploadFile(final MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            logger.warn("업로드된 파일이 비어 있습니다.");
            return null;
        }

        // 저장 파일명 생성
        String saveName = generateSaveFilename(multipartFile.getOriginalFilename());
        // 날짜 기반 디렉토리 생성
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        // 업로드 경로 설정
        Path uploadDirectory = Paths.get(uploadPath, today);

        try {
            // 파일 저장 디렉토리 확인 및 생성
            if (!Files.exists(uploadDirectory)) {
                Files.createDirectories(uploadDirectory);
                logger.info("디렉토리가 생성되었습니다: {}", uploadDirectory);
            }

            // 파일 저장 경로 설정
            Path filePath = uploadDirectory.resolve(saveName);
            multipartFile.transferTo(filePath.toFile());
            logger.info("파일 저장 성공: {}", filePath.toString());

            return FileRequest.builder()
                    .originalName(multipartFile.getOriginalFilename())
                    .saveName(today + "/" + saveName)  // 저장 경로를 포함하여 저장
                    .size(multipartFile.getSize())
                    .build();
        } catch (IOException e) {
            logger.error("파일 업로드 중 IOException 발생: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 저장 파일명 생성
     * @param filename 원본 파일명
     * @return 디스크에 저장할 파일명
     */
    private String generateSaveFilename(final String filename) {
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String extension = StringUtils.getFilenameExtension(filename);
        return uuid + "." + extension;
    }

    /**
     * 다운로드할 첨부파일(리소스) 조회 (as Resource)
     * @param file - 첨부파일 상세정보
     * @return 첨부파일(리소스)
     */
    public Resource readFileAsResource(final FileResponse file) {
        try {
            Path filePath = Paths.get(uploadPath, file.getSaveName());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isFile()) {
                throw new RuntimeException("file not found : " + filePath.toString());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("file not found : " + file.getSaveName());
        }
    }
}
