package com.example.androidsampleapp.ui.groupcall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidsampleapp.domain.model.GroupCallMember
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * グループ通話の画面の ViewModel。
 *
 * 画面を仮置きした段階なので、メンバーは見本の値を最初の State に入れている。
 * 通話の API ができたら、ほかの画面と同じく UseCase から購読して Intent で流す形にする。
 */
@HiltViewModel
class GroupCallViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(GroupCallState(members = SAMPLE_MEMBERS))
    val uiState: StateFlow<GroupCallState> = _uiState.asStateFlow()

    private val _effect = Channel<GroupCallEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val reducer = GroupCallReducer()

    /** 状態を変えうる入力の入口。Reducer で次の状態を作り、そのあと [handle] で副作用を実行する。 */
    fun onIntent(intent: GroupCallIntent) {
        var previous: GroupCallState
        var current: GroupCallState
        // 読んでから書くまでの間に別の更新が入っていたら、読み直してやり直す。
        do {
            previous = _uiState.value
            current = reducer.reduce(previous, intent)
        } while (!_uiState.compareAndSet(previous, current))
        viewModelScope.launch { handle(intent) }
    }

    private suspend fun handle(intent: GroupCallIntent) {
        when (intent) {
            GroupCallIntent.EndCallClicked -> _effect.send(GroupCallEffect.CallEnded)

            is GroupCallIntent.MicVolumeChanged,
            GroupCallIntent.SpeakerToggled,
            GroupCallIntent.MuteToggled,
            -> Unit
        }
    }

    private companion object {
        /** 仮置きの見本。一覧のスクロールを確かめられるよう、画面に収まらない人数にしてある。 */
        val SAMPLE_MEMBERS = listOf(
            GroupCallMember(id = "1", name = "1号車運転台", device = "ハンドセット"),
            GroupCallMember(id = "2", name = "添乗員端末2", device = "車両連絡"),
            GroupCallMember(id = "3", name = "2号車運転台", device = "ハンドセット"),
            GroupCallMember(id = "4", name = "添乗員端末1", device = "車両連絡"),
            GroupCallMember(id = "5", name = "指令室", device = "固定電話"),
            GroupCallMember(id = "6", name = "3号車運転台", device = "ハンドセット"),
            GroupCallMember(id = "7", name = "整備詰所", device = "固定電話"),
            GroupCallMember(id = "8", name = "添乗員端末3", device = "車両連絡"),
        )
    }
}
