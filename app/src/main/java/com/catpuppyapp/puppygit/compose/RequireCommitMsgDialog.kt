package com.catpuppyapp.puppygit.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.style.MyStyleKt
import com.catpuppyapp.puppygit.utils.Libgit2Helper
import com.github.git24j.core.Repository


@Composable
fun RequireCommitMsgDialog(
    repoPath:String,
    repoState:Int,
    overwriteAuthor:MutableState<Boolean>,
    amend:MutableState<Boolean>,
    commitMsg: MutableState<String>,
    indexIsEmptyForCommitDialog:MutableState<Boolean>,
    onOk: (msg:String) -> Unit,
    onCancel: () -> Unit,
) {
    val activityContext = LocalContext.current

    val repoStateIsRebase= repoState == Repository.StateT.REBASE_MERGE.bit

    val repoStateIsCherrypick = repoState == Repository.StateT.CHERRYPICK.bit

    fun getMsgEmptyNote():String {
        return activityContext.getString(if(repoStateIsRebase || repoStateIsCherrypick) R.string.leave_msg_empty_will_use_origin_commit_s_msg  else R.string.you_can_leave_msg_empty_will_auto_gen_one)
    }

    //勾选amend时用此变量替代commitMsg
    val amendMsg = rememberSaveable { mutableStateOf(commitMsg.value)}



    val view = LocalView.current
    val density = LocalDensity.current

    val isKeyboardVisible = remember { mutableStateOf(false) }
    //indicate keyboard covered component
    val isKeyboardCoveredComponent = remember { mutableStateOf(false) }
    // which component expect adjust heghit or padding when softkeyboard shown
    val componentHeight = remember { mutableIntStateOf(0) }
    // the padding value when softkeyboard shown
    val keyboardPaddingDp = remember { mutableStateOf(0.dp) }

    SoftkeyboardVisibleListener(
        view = view,
        isKeyboardVisible = isKeyboardVisible,
        isKeyboardCoveredComponent = isKeyboardCoveredComponent,
        componentHeight = componentHeight,
        keyboardPaddingDp = keyboardPaddingDp,
        density = density,
        paddingAdjustValue = 180.dp,
        skipCondition = { false }
    )


    AlertDialog(
        title = {
            Text(stringResource(R.string.commit_message))
        },
        text = {
            ScrollableColumn {
                if(repoState==Repository.StateT.NONE.bit && amend.value.not() && indexIsEmptyForCommitDialog.value) {
                    Row(modifier = Modifier.padding(5.dp)) {
                        Text(text = stringResource(R.string.warn_index_is_empty_will_create_a_empty_commit), color = MyStyleKt.TextColor.danger())
                    }
                }

                TextField(
                    modifier = Modifier.fillMaxWidth()
                        .onGloballyPositioned { layoutCoordinates ->
//                                println("layoutCoordinates.size.height:${layoutCoordinates.size.height}")
                            // 获取组件的高度
                            // unit is px ( i am not very sure)
                            componentHeight.intValue = layoutCoordinates.size.height
                        }
                        .then(
                            if (isKeyboardCoveredComponent.value) Modifier.padding(bottom = keyboardPaddingDp.value) else Modifier
                        )
                    ,

                    value = if(amend.value) amendMsg.value else commitMsg.value,
                    onValueChange = {
                        if(amend.value) {
                            amendMsg.value = it
                        }else {
                            commitMsg.value = it
                        }
                    },
                    label = {
                        Text(stringResource(R.string.commit_message))
                    },
                    placeholder = {
                        Text(stringResource(R.string.input_your_commit_message))
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    Text(text = "("+getMsgEmptyNote()+")",
                        color = MyStyleKt.TextColor.highlighting_green
                    )
                }

                //repo状态正常才显示amend
                if(repoState == Repository.StateT.NONE.bit) {
                    MyCheckBox(text = stringResource(R.string.amend), value = amend)
                }

                //正常来说这两个不会同时为真
                if(amend.value || repoStateIsRebase || repoStateIsCherrypick) {
                    Text(text=if(repoStateIsRebase || repoStateIsCherrypick) stringResource(R.string.origin_commit_msg) else stringResource(R.string.last_commit_msg),
                        style = MyStyleKt.ClickableText.style,
                        color = MyStyleKt.ClickableText.color,
                        fontWeight = FontWeight.Light,
                        modifier = MyStyleKt.ClickableText.modifierNoPadding
                            .padding(horizontal = MyStyleKt.CheckoutBox.horizontalPadding)
                            .clickable(onClick = {
                                Repository.open(repoPath).use { repo ->
                                    val oldMsg = if (repoStateIsRebase) Libgit2Helper.rebaseGetCurCommitMsg(repo) else if(repoStateIsCherrypick) Libgit2Helper.getCherryPickHeadCommitMsg(repo) else Libgit2Helper.getHeadCommitMsg(repo)

                                    if(amend.value) {
                                        amendMsg.value = oldMsg
                                    }else {
                                        commitMsg.value = oldMsg
                                    }
                                }
                            })
                    )
                }


                // amend或rebase时可覆盖旧commit的作者信息，按钮一样，但含义不同，普通状态下commit覆盖的是上一个commit的信息，rebase状态下覆盖的是被pick的commit的信息
                if(amend.value || repoStateIsRebase || repoStateIsCherrypick) {
                    MyCheckBox(text = stringResource(R.string.overwrite_author), value = overwriteAuthor)
                }

                if(overwriteAuthor.value){
                    Text(text = stringResource(R.string.will_use_your_username_and_email_overwrite_original_commits_author_info),
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(horizontal = MyStyleKt.CheckoutBox.horizontalPadding)
                        )
                }

            }

        },
        onDismissRequest = {
            onCancel()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOk(if(amend.value) amendMsg.value else commitMsg.value)
                },
                enabled = true
            ) {
                Text(stringResource(id = R.string.ok))
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onCancel()
                }
            ) {
                Text(stringResource(id = R.string.cancel))
            }
        }
    )
}

