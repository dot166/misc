package io.github.dot166.libphone2.spn

import android.os.Parcel
import android.os.Parcelable

class Item : Parcelable {
    val number: String
    val name: String?
    val categories: String?
    val languages: String?
    val organization: String?
    val website: String?

    constructor(
        number: String, name: String?, categories: String?, languages: String?,
        organization: String?, website: String?
    ) {
        this.number = number
        this.name = name
        this.categories = categories
        this.languages = languages
        this.organization = organization
        this.website = website
    }

    private constructor(`in`: Parcel) {
        number = `in`.readString()!!
        name = `in`.readString()
        categories = `in`.readString()
        languages = `in`.readString()
        organization = `in`.readString()
        website = `in`.readString()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(number)
        dest.writeString(name)
        dest.writeString(categories)
        dest.writeString(languages)
        dest.writeString(organization)
        dest.writeString(website)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Item?> = object : Parcelable.Creator<Item?> {
            override fun createFromParcel(`in`: Parcel): Item {
                return Item(`in`)
            }

            override fun newArray(size: Int): Array<Item?> {
                return arrayOfNulls(size)
            }
        }
    }
}