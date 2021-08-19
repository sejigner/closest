package com.sejigner.closest.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.database.FirebaseDatabase
import com.sejigner.closest.ChatLogActivity
import com.sejigner.closest.R
import com.sejigner.closest.UI.FragmentChatViewModel
import com.sejigner.closest.UI.FragmentChatViewModelFactory
import com.sejigner.closest.room.FirstPaperPlanes
import com.sejigner.closest.room.PaperPlaneDatabase
import com.sejigner.closest.room.PaperPlaneRepository
import com.sejigner.closest.room.RepliedPaperPlanes
import kotlinx.android.synthetic.main.fragment_dialog_first.*
import kotlinx.android.synthetic.main.fragment_dialog_second.*
import java.text.SimpleDateFormat
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ITEMS = "data"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDialogReplied.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentDialogReplied : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var partnerMessage: String? = null
    private var distance: String? = null
    private var replyTime: Long? = null
    private var fromId: String?= null
    private var paper : RepliedPaperPlanes?= null
    private var userMessage : String? = null
    private var firstTime : Long?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            partnerMessage = it.getString("partnerMessage")
            distance = it.getString("distance")
            replyTime = it.getLong("replyTime")
            fromId = it.getString("fromId")
            userMessage = it.getString("userMessage")
            firstTime = it.getLong("firstTime")
        }
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dialog_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = PaperPlaneRepository(PaperPlaneDatabase(requireActivity()))
        val factory = FragmentChatViewModelFactory(repository)
        val viewModel = ViewModelProvider(requireActivity(), factory).get(FragmentChatViewModel::class.java)


        tv_dialog_message_second.text = partnerMessage
        tv_dialog_distance_second.text = distance

        setDateToTextView(replyTime!!)

        // 버리기 -> 파이어베이스 데이터 삭제
        tv_dialog_discard_second.setOnClickListener {
            viewModel.delete(paper!!)
            dismiss()
        }


        tv_dialog_start_chat.setOnClickListener {
            // 답장을 할 경우 메세지는 사라지고, 채팅으로 넘어가는 점 숙지시킬 것 (Dialog 이용)
            val intent = Intent(view.context,ChatLogActivity::class.java)
            intent.putExtra(FragmentChat.USER_KEY, fromId)
            startActivity(intent)
            viewModel.delete(paper!!)
            dismiss()
        }
    }


    private fun setDateToTextView(timestamp: Long) {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm")
        sdf.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        val date = sdf.format(timestamp*1000L)
        tv_dialog_time_second.text = date.toString()
    }

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FragmentDialog.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(paperPlane : RepliedPaperPlanes) =
            FragmentDialogReplied().apply {
                arguments = Bundle().apply {
                    putString("partnerMessage", paperPlane.partnerMessage)
                    putString("distance", paperPlane.flightDistance.toString())
                    putLong("replyTime", paperPlane.replyTimestamp)
                    putLong("firstTime",paperPlane.firstTimestamp)
                    putString("fromId", paperPlane.fromId)
                    putString("userMessage", paperPlane.userMessage)
                }
                paper = paperPlane
            }
    }
}