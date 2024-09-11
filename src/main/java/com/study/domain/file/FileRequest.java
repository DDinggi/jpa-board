package com.study.domain.file;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileRequest {
    private Long postId;  // 게시물 ID 필드 추가
    private String originalName;
    private String saveName;
    private long size;
}
