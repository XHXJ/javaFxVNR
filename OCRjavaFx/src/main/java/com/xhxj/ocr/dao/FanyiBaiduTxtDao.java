package com.xhxj.ocr.dao;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class FanyiBaiduTxtDao {
    /**
     * src : 原文
     * dst : 译文
     */

    private String src;
    private String dst;
}
