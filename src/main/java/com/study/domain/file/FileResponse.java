package com.study.domain.file;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FileResponse {

    private Long id;                      // 파일 번호 (PK)
    private Long postId;                  // 게시글 번호 (FK)
    private String originalName;          // 원본 파일명
    private String saveName;              // 저장 파일명
    private long size;                    // 파일 크기
    private Boolean deleteYn;             // 삭제 여부
    private LocalDateTime createdDate;    // 생성일시
    private LocalDateTime deletedDate;    // 삭제일시

}
