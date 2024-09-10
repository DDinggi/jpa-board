package com.study.domain.file;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);
    private final FileMapper fileMapper;
    private final String uploadPath = "C:/develop/upload-files"; // 기본 파일 업로드 경로

    @Transactional
    public void saveFile(Long postId, MultipartFile file) {
        if (file.isEmpty()) {
            logger.info("업로드된 파일이 비어 있습니다. 파일 저장을 건너뜁니다.");
            return;
        }

        try {
            // 현재 날짜 기반 폴더명 생성 (예: 240801)
            String folderName = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
            Path folderPath = Paths.get(uploadPath, folderName);

            // 폴더가 존재하지 않으면 생성
            if (!Files.exists(folderPath)) {
                Files.createDirectories(folderPath);
                logger.info("폴더 생성: {}", folderPath);
            }

            // 저장 파일명 생성
            String saveName = generateSaveFilename(file.getOriginalFilename());

            // 파일 저장 경로 설정
            Path filePath = folderPath.resolve(saveName);

            // 파일을 디스크에 저장
            file.transferTo(filePath.toFile());
            logger.info("파일이 디스크에 저장되었습니다: {}", filePath.toString());

            // 파일 정보 저장 (DB에 저장하기 위한 객체 생성)
            FileRequest fileRequest = FileRequest.builder()
                    .postId(postId)
                    .originalName(file.getOriginalFilename())
                    .saveName(folderName + "/" + saveName) // 저장 경로를 DB에 함께 저장
                    .size(file.getSize())
                    .build();

            try {
                fileMapper.saveFile(fileRequest); // 파일 정보를 데이터베이스에 저장
                logger.info("파일 정보가 데이터베이스에 저장되었습니다: {}", fileRequest);
            } catch (Exception dbException) {
                logger.error("파일 정보를 데이터베이스에 저장하는 중 오류 발생: {}, SQL 상태: {}", dbException.getMessage(), dbException.getCause());
                throw new RuntimeException("파일 정보를 데이터베이스에 저장하는 중 오류 발생: " + dbException.getMessage(), dbException);
            }

        } catch (IOException e) {
            logger.error("파일 저장 중 오류 발생: 경로 - {}, 파일명 - {}, 오류 메시지: {}", uploadPath, file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("파일 저장 중 오류 발생: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void saveFiles(final Long postId, final List<FileRequest> files) {
        if (CollectionUtils.isEmpty(files)) {
            logger.info("파일 목록이 비어 있습니다. 파일 저장을 건너뜁니다.");
            return;
        }
        for (FileRequest file : files) {
            file.setPostId(postId);
            logger.info("파일 저장 준비 중 - 원본 파일명: {}, 저장 파일명: {}", file.getOriginalName(), file.getSaveName());
        }
        try {
            fileMapper.saveAll(files); // 다수의 파일 정보를 데이터베이스에 저장
            logger.info("파일 정보가 데이터베이스에 저장되었습니다. 저장된 파일 수: {}", files.size());
        } catch (Exception dbException) {
            logger.error("파일 정보를 데이터베이스에 저장하는 중 오류 발생: {}, SQL 상태: {}", dbException.getMessage(), dbException.getCause());
            throw new RuntimeException("파일 정보를 데이터베이스에 저장하는 중 오류 발생: " + dbException.getMessage(), dbException);
        }
    }

    /**
     * 파일 리스트 조회
     * @param postId - 게시글 번호 (FK)
     * @return 파일 리스트
     */
    public List<FileResponse> findAllFileByPostId(Long postId) {
        List<FileResponse> files = fileMapper.findAllByPostId(postId);
        logger.info("파일 리스트 조회 완료. 게시글 ID: {}, 조회된 파일 수: {}", postId, files.size());
        return files;
    }

    /**
     * 다수의 파일 리스트 조회
     * @param ids - 파일 ID 리스트
     * @return 파일 리스트
     */
    public List<FileResponse> findAllFileByIds(final List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            logger.info("파일 ID 목록이 비어 있습니다. 조회를 건너뜁니다.");
            return Collections.emptyList();
        }
        List<FileResponse> files = fileMapper.findAllByIds(ids);
        logger.info("파일 리스트 조회 완료. 조회된 파일 수: {}", files.size());
        return files;
    }

    /**
     * 파일 상세정보 조회
     * @param id - 파일 ID (PK)
     * @return 파일 상세정보
     */
    public FileResponse findFileById(Long id) {
        FileResponse fileResponse = fileMapper.findById(id);
        if (fileResponse != null) {
            logger.info("파일 상세정보 조회 완료. 파일 ID: {}, 파일명: {}", id, fileResponse.getOriginalName());
        } else {
            logger.warn("파일 상세정보를 찾을 수 없습니다. 파일 ID: {}", id);
        }
        return fileResponse;
    }

    /**
     * 파일 삭제 (from Database)
     * @param ids - 파일 ID 리스트
     */
    @Transactional
    public void deleteAllFileByIds(final List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            logger.info("삭제할 파일 ID 목록이 비어 있습니다. 삭제를 건너뜁니다.");
            return;
        }
        fileMapper.deleteAllByIds(ids);
        logger.info("파일 삭제 완료. 삭제된 파일 ID 수: {}", ids.size());
    }

    /**
     * 저장 파일명 생성
     * @param originalFilename 원본 파일명
     * @return UUID로 생성된 새로운 파일명
     */
    private String generateSaveFilename(String originalFilename) {
        String uuid = UUID.randomUUID().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        return uuid + extension;
    }
}
