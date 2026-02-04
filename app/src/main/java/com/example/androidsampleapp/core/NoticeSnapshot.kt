package com.example.androidsampleapp.core

import com.example.androidsampleapp.domain.model.Notice

/** 通知一覧の今の姿。画面はこれをそのまま描く。持ち主は [MissedCallManager]。 */
data class NoticeSnapshot(
    /** 新しい順に並べた一覧。 */
    val notices: List<Notice> = emptyList(),
    /**
     * 通知を一度でも受け取れたか。
     * 「まだ取っていない空」と「取ったら空だった」を分けるために持つ。
     */
    val isLoaded: Boolean = false,
    /** 直近の取得（または消去）が失敗したか。失敗しても受け取り済みの一覧は [notices] に残る。 */
    val loadFailed: Boolean = false,
) {
    /**
     * 通知をまだ一度も受け取れておらず、失敗もしていない間。
     *
     * 取得の完了を待たずに、受け取れたかどうかから決める。StateFlow は同じ値を
     * 入れ直しても流れないので、「流れてきたら読み込み終わり」にすると取り直しで止まるため。
     */
    val isLoading: Boolean
        get() = !isLoaded && !loadFailed
}
