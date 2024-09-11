package com.study.domain.file;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {

    // 파일 정보를 저장하는 메서드 추가
    void saveFile(FileRequest fileRequest);

    // 다수의 파일 정보를 저장하는 메서드
    void saveAll(@Param("files") List<FileRequest> files);

    // 특정 게시물의 모든 파일을 조회하는 메서드
    List<FileResponse> findAllByPostId(@Param("postId") Long postId);

    // 특정 ID로 파일들을 조회하는 메서드
    List<FileResponse> findAllByIds(@Param("ids") List<Long> ids);

    // 특정 파일을 조회하는 메서드
    FileResponse findById(@Param("fileId") Long fileId);

    // 다수의 파일을 삭제하는 메서드
    void deleteAllByIds(@Param("ids") List<Long> ids);
}
