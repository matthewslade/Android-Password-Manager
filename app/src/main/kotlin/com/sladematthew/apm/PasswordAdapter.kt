package com.sladematthew.apm

import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Filter
import android.widget.Filterable
import com.sladematthew.apm.model.Password

import com.sladematthew.apm.model.PasswordList
import kotlinx.android.synthetic.main.item_password.view.*
import java.util.*
import java.util.regex.Pattern

class PasswordAdapter(var passwords: List<Password>,var onItemClickListener: OnItemClickListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

    var filtered:List<Password>?=null


    override fun getFilter(): Filter? {
        return object :Filter()
        {
            override fun publishResults(constraint: CharSequence?, results: Filter.FilterResults?) {
                setAndSortData(results!!.values as ArrayList<Password>)
                notifyDataSetChanged()
            }

            override fun performFiltering(constraint: CharSequence?): Filter.FilterResults? {
                val filterResults = Filter.FilterResults()
                val tmp = ArrayList<Password>()
                if (constraint == null || "" == constraint)
                {
                    tmp.addAll(passwords)
                }
                else
                {
                    for (item in passwords)
                    {
                        if (matchString(item.label, constraint))
                            tmp.add(item)
                    }
                }
                filterResults.values = tmp
                filterResults.count = tmp.size
                return filterResults
            }

        }
    }

    init
    {
        setAndSortData(passwords)
    }

    fun setAndSortData(list:List<Password>)
    {
        filtered = list
        Collections.sort(filtered,{ lhs, rhs -> lhs.label.compareTo(rhs.label)});
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder? {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_password, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).itemView.name.text = filtered!![position].label
    }

    override fun getItemCount(): Int {
        return filtered!!.size;
    }

    interface OnItemClickListener {
        fun onClick(viewHolder: ViewHolder, item: Password, position: Int)
        fun onLongClick(viewHolder: ViewHolder, item: Password, position: Int)
    }

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view),View.OnClickListener, View.OnLongClickListener
    {
        init
        {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onLongClick(p0: View?): Boolean
        {
            var pos = layoutPosition
            if (pos < 0) {
                pos = 0
            }
            onItemClickListener.onLongClick(this,filtered!![pos], pos)
            return true
        }

        override fun onClick(v: View)
        {
            var pos = layoutPosition
            if (pos < 0) {
                pos = 0
            }
            onItemClickListener.onClick(this,filtered!![pos], pos)
        }
    }

    fun matchString(value:String,constraint: CharSequence?):Boolean{
        if (TextUtils.isEmpty(value)) {
            return false
        }
        var s = constraint.toString()
        if (TextUtils.isEmpty(s)) {
            return false
        }
        s = Pattern.quote(s)
        return Pattern.compile("$s.*|.*[-\\s/]$s.*", Pattern.CASE_INSENSITIVE).matcher(value).matches()
    }
}
