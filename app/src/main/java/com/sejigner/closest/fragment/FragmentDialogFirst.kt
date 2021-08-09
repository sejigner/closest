package com.sejigner.closest.fragment

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.firebase.database.FirebaseDatabase
import com.sejigner.closest.R
import com.sejigner.closest.models.PaperplaneMessage
import kotlinx.android.synthetic.main.fragment_dialog_first.*
import java.text.SimpleDateFormat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ITEMS = "data"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentDialogFirst.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentDialogFirst : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var message: String? = null
    private var distance: String? = null
    private var time: Long? = null
    private var toId: String? = null
    private var fromId: String?= null
    private var isReplied: Boolean ?= null
    private var replyTime: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            message = it.getString("message")
            distance = it.getString("distance")
            time = it.getLong("time")
            toId = it.getString("toId")
            fromId = it.getString("fromId")
            isReplied = it.getBoolean("isReplied")



        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_dialog_first, container, false)
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val etReply = view.findViewById<View>(R.id.et_dialog_message_first) as? EditText
        var textEntered = ""

        val btnCancel = view.findViewById<View>(R.id.iv_back_reply_first) as? ImageView
        val btnDiscard = view.findViewById<View>(R.id.tv_dialog_discard_first) as? TextView
        val btnReply = view.findViewById<View>(R.id.tv_dialog_send) as? TextView

        setDateToTextView(time!!)



        btnCancel?.setOnClickListener {
            dismiss()
        }

        btnDiscard?.setOnClickListener {
            // Firebase 내 해당 데이터 삭제
            removePaper()
            dismiss()
        }

        tv_dialog_message_first.text = message
        tv_dialog_distance_first.text = distance.toString()



        btnReply?.setOnClickListener {
            textEntered = etReply?.text.toString()
            if(textEntered != "") {
                val paperPlaneReceiverReference =
                    FirebaseDatabase.getInstance().getReference("/PaperPlanes/Receiver/$fromId/$toId")
                val paperplaneMessage = PaperplaneMessage(
                    paperPlaneReceiverReference.key!!,
                    textEntered,
                    toId!!,
                    fromId!!,
                    distance!!.toDouble(),
                    System.currentTimeMillis() / 1000L,
                    true)
                paperPlaneReceiverReference.setValue(paperplaneMessage).addOnFailureListener {
                    Log.d(TAG, "Reply 실패")
                }.addOnSuccessListener {
                    Toast.makeText(
                        requireActivity(),
                        "당신의 답장 종이비행기가 ${distance}m 거리의 누군가에게 도달했어요!",
                        Toast.LENGTH_LONG
                    ).show()
                    removePaper()
                    dismiss()
                    removePaper()
                }


            } else {
                Toast.makeText(requireActivity(), "메세지를 입력해주세요.",Toast.LENGTH_SHORT ).show()
            }
        }

        tv_dialog_discard_first.setOnClickListener {
            removePaper()
            dismiss()
        }
    }

    private fun removePaper() {
        val paperPlaneReceiverReference =
            FirebaseDatabase.getInstance().getReference("/PaperPlanes/Receiver/$toId/$fromId")
        paperPlaneReceiverReference.removeValue()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog!!.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog!!.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }

    private fun setDateToTextView(timestamp: Long) {
        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm")
        val date = sdf.format(timestamp)
        tv_dialog_time_first.text = date.toString()
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

        const val TAG = "PaperDialog"

        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(message: String, distance : String, time: Long, toId: String, fromId: String, isReplied: Boolean ) =
            FragmentDialogFirst().apply {
                arguments = Bundle().apply {
                    putString("message", message)
                    putString("distance", distance)
                    putLong("time", time)
                    putString("toId", toId)
                    putString("fromId", fromId)
                    putBoolean("isReplied", isReplied)
                }
            }
    }
}