package com.saba.bulletjournal

import android.os.Parcel
import android.os.Parcelable
import java.util.*

data class Note(
    var documentId: String = "",
    var title: String = "",
    var content: String = "",
    var userId: String = "",
    var date: String = "",
    var isChecked: Boolean = false // اضافه کردن متغیر isChecked

) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte() // خواندن isChecked از Parcel
    )
    fun compareTo(other: Note): Int {
        return other.date.compareTo(date)
    }
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(documentId)
        parcel.writeString(title)
        parcel.writeString(content)
        parcel.writeString(userId)
        parcel.writeString(date)
        parcel.writeByte(if (isChecked) 1 else 0) // نوشتن isChecked به Parcel
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Note> {
        override fun createFromParcel(parcel: Parcel): Note {
            return Note(parcel)
        }

        override fun newArray(size: Int): Array<Note?> {
            return arrayOfNulls(size)
        }

    }

}

