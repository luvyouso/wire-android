/**
 * Wire
 * Copyright (C) 2017 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.waz.zclient.views

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.widget.RelativeLayout
import android.widget.TextView.OnEditorActionListener
import com.waz.zclient.ui.text.TypefaceTextView
import com.waz.zclient.utils.RichView
import com.waz.zclient.views.PickerSpannableEditText.Callback
import com.waz.zclient.{R, ViewHelper}

class SearchEditText(context: Context, attrs: AttributeSet, style: Int) extends RelativeLayout(context, attrs, style) with ViewHelper {
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null, 0)

  inflate(R.layout.search_edit_text)

  protected val searchBox = findById[PickerSpannableEditText](R.id.edit_text)
  protected val searchHint = findById[TypefaceTextView](R.id.hint)
  private var callback = Option.empty[Callback]
  private val styleAttributes: TypedArray = context.getTheme.obtainStyledAttributes(attrs, R.styleable.SearchEditText, 0, 0)

  searchHint.setText(styleAttributes.getString(R.styleable.SearchEditText_hintText))
  searchBox.setCallback(new Callback {
    override def afterTextChanged(s: String) = {
      callback.foreach(_.afterTextChanged(s))
      updateInternal()
    }
    override def onRemovedTokenSpan(element: PickableElement) = {
      callback.foreach(_.onRemovedTokenSpan(element))
      updateInternal()
    }
  })

  def addElement(pickableElement: PickableElement) = {
    searchBox.addElement(pickableElement)
    updateInternal()
  }

  def removeElement(pickableElement: PickableElement) = {
    searchBox.removeElement(pickableElement)
    updateInternal()
  }

  def setCallback(callback: PickerSpannableEditText.Callback) = this.callback = Option(callback)

  private def updateInternal(): Unit = {
      searchHint.setVisible(searchBox.getSearchFilter.isEmpty && Option(searchBox.getElements).forall(_.isEmpty))
  }

  def getSearchFilter = searchBox.getSearchFilter

  def setCursorColor(accentColor: Int) = searchBox.setAccentColor(accentColor)

  def setTextColor(color: Int) = searchBox.setTextColor(color)

  def setOnEditorActionListener(listener: OnEditorActionListener) = searchBox.setOnEditorActionListener(listener)

  def setFocus(): Unit = searchBox.requestFocus()

  def getElements: Set[PickableElement] = Option(searchBox.getElements).getOrElse(Set())
}
