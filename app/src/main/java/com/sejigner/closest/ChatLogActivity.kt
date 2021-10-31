package com.sejigner.closest

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.sejigner.closest.Adapter.ChatLogAdapter
import com.sejigner.closest.MainActivity.Companion.MYNICKNAME
import com.sejigner.closest.MainActivity.Companion.UID
import com.sejigner.closest.fragment.FragmentChat
import com.sejigner.closest.fragment.FragmentDialogReplied
import com.sejigner.closest.fragment.FragmentDialogReportChat
import com.sejigner.closest.models.ChatMessage
import com.sejigner.closest.models.LatestChatMessage
import com.sejigner.closest.room.ChatMessages
import com.sejigner.closest.room.PaperPlaneDatabase
import com.sejigner.closest.room.PaperPlaneRepository
import com.sejigner.closest.ui.FragmentChatViewModel
import com.sejigner.closest.ui.FragmentChatViewModelFactory
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class ChatLogActivity : AppCompatActivity(), FragmentDialogReplied.RepliedPaperListener {

    companion object {
        const val TAG = "ChatLog"
    }

    private var fbDatabase: FirebaseDatabase? = null
    private var partnerUid: String? = null
    private var partnerFcmToken : String? = null
    lateinit var ViewModel: FragmentChatViewModel
    lateinit var chatLogAdapter: ChatLogAdapter
    lateinit var partnerNickname : String
    private val FCM_MESSAGE_URL = "https://fcm.googleapis.com/fcm/send"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        val repository = PaperPlaneRepository(PaperPlaneDatabase(this))
        val factory = FragmentChatViewModelFactory(repository)
        ViewModel = ViewModelProvider(this, factory)[FragmentChatViewModel::class.java]

        fbDatabase = FirebaseDatabase.getInstance()

        updatePartnersToken()
        partnerUid = intent.getStringExtra(FragmentChat.USER_KEY)
        chatLogAdapter = ChatLogAdapter(listOf(), ViewModel)
        rv_chat_log.adapter = chatLogAdapter


        val mLayoutManagerMessages = LinearLayoutManager(this)
        mLayoutManagerMessages.orientation = LinearLayoutManager.VERTICAL

        rv_chat_log.layoutManager = mLayoutManagerMessages
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        ViewModel.allChatMessages(UID, partnerUid!!).observe(this, {
            chatLogAdapter.list = it
            chatLogAdapter.notifyDataSetChanged()
        })

//        ViewModel.allChatMessages(partnerUid!!).observe(this, {
//            chatLogAdapter.list = it
//            chatLogAdapter.notifyDataSetChanged()
//        })

        CoroutineScope(IO).launch {
            if (!partnerUid.isNullOrBlank()) {
                val chatRoom = ViewModel.getChatRoom(partnerUid!!).await()
                tv_partner_nickname_chat_log.text = chatRoom.partnerNickname
                partnerNickname = chatRoom.partnerNickname!!
            }
        }


//        val ref = fbDatabase?.reference?.child("Users")?.child(partnerUid!!)?.child("strNickname")
//            ref?.get()?.addOnSuccessListener {
//            supportActionBar?.title = it.value.toString()
//        }

        listenForMessages()

        // 보내기 버튼 초기상태 false / 입력시 활성화
        btn_send_chat_log.isEnabled = false
        et_message_chat_log.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                if (p0.toString().trim { it <= ' ' }.isEmpty()) {
                    btn_send_chat_log.isEnabled = false
                    btn_send_chat_log.setBackgroundColor(resources.getColor(R.color.txt_gray))
                }

            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString().trim { it <= ' ' }.isNotEmpty()) {
                    btn_send_chat_log.isEnabled = true
                    btn_send_chat_log.setBackgroundColor(resources.getColor(R.color.paperplane_theme))
                }
            }
        })

        btn_send_chat_log.setOnClickListener {
            performSendMessage()
            et_message_chat_log.text.clear()
            rv_chat_log.scrollToPosition(chatLogAdapter.itemCount - 1)
        }

        iv_back_chat_log.setOnClickListener {
            super.onBackPressed()
        }

        btn_leave_menu_chat_log.setOnClickListener {
            CoroutineScope(IO).launch {
                val chatroom = ViewModel.getChatRoom(partnerUid!!).await()
                ViewModel.delete(chatroom)
            }
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        iv_menu_chat_log.setOnClickListener {
            menuToggle()
        }

        btn_report_menu_chat_log.setOnClickListener {
            val dialog = FragmentDialogReportChat()
            val fm = supportFragmentManager
            dialog.show(fm, "reportChatMessage")
        }


    }

    private fun setPartnersFcmToken() {
        val ref = FirebaseDatabase.getInstance().getReference("/Users/$partnerUid/registrationToken")
        ref.get()
            .addOnSuccessListener { it ->
                partnerFcmToken = it.value.toString()
            }.addOnFailureListener {
              Log.d("ChatLogActivity", it.message!!)
            }
    }

    private fun updatePartnersToken() {
        val ref = FirebaseDatabase.getInstance().getReference("/Users/$partnerUid/registrationToken")

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                partnerFcmToken = snapshot.value.toString()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                partnerFcmToken = snapshot.value.toString()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun menuToggle() {
        if (expandable_menu_chat_log.visibility == View.VISIBLE) {
            expandable_menu_chat_log.visibility  = View.GONE
        } else {
            expandable_menu_chat_log.visibility = View.VISIBLE
        }
    }

    private fun listenForMessages() {

            val ref = FirebaseDatabase.getInstance().getReference("/User-messages/$UID/$partnerUid")

            ref.addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val chatMessage = snapshot.getValue(ChatMessage::class.java)
                    val currentMessageDate = getDateTime(chatMessage!!.timestamp)
                    val partnerUid: String = setPartnerId(chatMessage.fromId, chatMessage)
                    val isPartner = setSender(chatMessage.fromId)


                    val chatMessages = ChatMessages(
                        null,
                        partnerUid,
                        UID,
                        isPartner,
                        chatMessage.message,
                        chatMessage.timestamp
                    )


                    CoroutineScope(IO).launch {
                        var lastMessageTimeStamp : Long? = 0L
                        var lastMessageDate : String?

                        lastMessageTimeStamp = ViewModel.getChatRoomsTimestamp(UID, partnerUid).await()
                        lastMessageDate = getDateTime(lastMessageTimeStamp!!)

                        if (!lastMessageDate.equals(currentMessageDate)) {
                            lastMessageDate = currentMessageDate
                            val dateMessage = ChatMessages(null,partnerUid, UID,2,lastMessageDate,0L)
                            ViewModel.insert(dateMessage).join()
                        }

                        ViewModel.insert(chatMessages)
                        ref.child(snapshot.key!!).removeValue()

                    }
                    rv_chat_log.scrollToPosition(chatLogAdapter.itemCount - 1)

                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onChildRemoved(snapshot: DataSnapshot) {

                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onCancelled(error: DatabaseError) {

                }

            })
    }

    private fun setPartnerId(fromId: String, chatMessage: ChatMessage): String {
        if (fromId == UID) {
            return chatMessage.toId
        } else return chatMessage.fromId
    }

    private fun setSender(partnerId: String): Int {
        return if(partnerId != UID) 1
        else 0
    }

    private fun getDateTime(time: Long): String? {
        try {
            val sdf = SimpleDateFormat("yyyy년 MM월 dd일")
            sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
            val netDate = Date(time * 1000)
            return sdf.format(netDate)
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
            return e.toString()
        }
    }


    private fun performSendMessage() {
        val text = et_message_chat_log.text.toString()
        val fromId = UID
        val toId = partnerUid
        val timestamp = System.currentTimeMillis() / 1000
        val currentMessageDate = getDateTime(timestamp)



        val toRef =
            FirebaseDatabase.getInstance().getReference("/User-messages/$toId/$fromId").push()
        val chatMessage = ChatMessage(toRef.key!!, UID, text, fromId, toId!!, timestamp)
        toRef.setValue(chatMessage).addOnSuccessListener {
            Log.d(TAG, "sent your message: ${toRef.key}")
        }

        val lastMessagesUserReference =
            FirebaseDatabase.getInstance().getReference("/Last-messages/$UID/$toId")
        val lastMessageToMe = LatestChatMessage(partnerNickname,text,timestamp)
        lastMessagesUserReference.setValue(lastMessageToMe)

        val lastMessagesPartnerReference =
            FirebaseDatabase.getInstance().getReference("/Last-messages/$toId/$UID")
        val lastMessageToPartner = LatestChatMessage(MYNICKNAME,text,timestamp)
        lastMessagesPartnerReference.setValue(lastMessageToPartner)


        val chatMessages = ChatMessages(null, toId, UID, 0, text, timestamp)

        CoroutineScope(IO).launch {
            var lastMessageTimeStamp: Long? = 0L
            var lastMessageDate: String?

            lastMessageTimeStamp = ViewModel.getChatRoomsTimestamp(UID, partnerUid!!).await()
            lastMessageDate = getDateTime(lastMessageTimeStamp!!)

            if (!lastMessageDate.equals(currentMessageDate)) {
                lastMessageDate = currentMessageDate
                val dateMessage = ChatMessages(null, partnerUid, UID, 2, lastMessageDate, 0L)
                ViewModel.insert(dateMessage).join()
            }
            ViewModel.insert(chatMessages)
        }
    }

    override fun initChatLog() {
        val timestamp = System.currentTimeMillis() / 1000

        val noticeMessage = ChatMessages(null, partnerUid, UID, 2, getString(R.string.init_chat_log),0L)
        ViewModel.insert(noticeMessage)

        val lastMessagesUserReference =
            FirebaseDatabase.getInstance().getReference("/Last-messages/$UID/$partnerUid")
        val lastMessageToMe = LatestChatMessage(partnerNickname,getString(R.string.init_chat_log),timestamp)
        lastMessagesUserReference.setValue(lastMessageToMe)

        val lastMessagesPartnerReference =
            FirebaseDatabase.getInstance().getReference("/Last-messages/$partnerUid/$UID")
        val lastMessageToPartner = LatestChatMessage(MYNICKNAME,getString(R.string.init_chat_log),timestamp)
        lastMessagesPartnerReference.setValue(lastMessageToPartner)

    }

    fun reportMessagesFirebase() {
        // TODO : 신고 시 List<ChatMessages> -> Firebase 업로드
        CoroutineScope(IO).launch {
            // TODO : chatRoomAndAllMessages 중첩된 관계 정의 (https://developer.android.com/training/data-storage/room/relationships)
            val messageList = ViewModel.chatRoomAndAllMessages(UID, partnerUid!!).await()
            val reportRef =
                FirebaseDatabase.getInstance().getReference("/Reports/Chat/$UID/$partnerUid")
            reportRef.setValue(messageList).addOnFailureListener {
                Log.d("ReportChatLog", "Report 실패")
            }.addOnSuccessListener {
                Log.d("Report","신고가 접수되었어요")
                ViewModel.deleteAllMessages(UID, partnerUid!!)
                ViewModel.deleteChatRoom(UID, partnerUid!!)
                startActivity(Intent(this@ChatLogActivity,MainActivity::class.java))
            }
        }

//        ViewModel.allChatMessages(partnerUid!!).observe(this, {
//            val reportRef =
//                FirebaseDatabase.getInstance().getReference("/ChatReport/$UID")
//            reportRef.setValue(it)
//        })
    }
}

//class ChatFromItem(val text: String, val time: Long) : Item<GroupieViewHolder>() {
//
//    private var lastMessageTimeMe: String? = null
//
//    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
//        viewHolder.itemView.tv_message_me.text = text
//        viewHolder.itemView.tv_time_me.text = setTime(time)
//    }
//
//    override fun getLayout(): Int {
//        return R.layout.chat_me_row
//    }
//
//    private fun setTime(timestamp: Long): String {
//        val sdf = SimpleDateFormat("a hh:mm")
//        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
//        val date = sdf.format(timestamp * 1000L)
//        return date.toString()
//    }
//
//
//}
//
//class ChatToItem(val text: String, val time: Long) : Item<GroupieViewHolder>() {
//
//    private var lastMessageTimePartner: String? = null
//
//    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
//        viewHolder.itemView.tv_message_partner.text = text
//        viewHolder.itemView.tv_time_partner.text = setTime(time)
//    }
//
//    override fun getLayout(): Int {
//        return R.layout.chat_partner_row
//    }
//
//    private fun setTime(timestamp: Long): String {
//        val sdf = SimpleDateFormat("a hh:mm")
//        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
//        val date = sdf.format(timestamp * 1000L)
//        return date.toString()
//    }
//
//}
//
//class ChatDate(private val lastDate: String) : Item<GroupieViewHolder>() {
//    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
//        viewHolder.itemView.tv_chat_date.text = lastDate
//    }
//
//    override fun getLayout(): Int {
//        return R.layout.chat_date
//    }
//
//}

