package com.example.UI

/**
 * 进度条对外接口，定义可配置的最小属性集。
 */
interface IProgressStrip {
    /** 最小值 */
    var min: Int
    /** 最大值 */
    var max: Int
    /** 当前进度值 */
    var value: Int
    /** 进度条背景色 */
    var progressBackground: Int
    /** 显示文字 */
    var text: String
}
