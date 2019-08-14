package com.xhxj.ocr.dao;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
@Data
public class FanyiBaiduDao {

    /**
     * from : jp 指定语言
     * to : zh 翻译语言
     * trans_result : [{"src":"生活の文化を笑颜であるし、何もない滞纳私たちは、常にする必要があるので苦い。","dst":"生活文化就是微笑，什么都没有滞纳，所以我们要一直苦下去。"}]
     */

    private String from;
    private String to;
    private List<FanyiBaiduTxtDao> trans_result;
}
