package controllers

import play.api.data.Form

import scalaz.\/

object Helper {

  implicit class RichForm[T](form: Form[T]) {
    def toEither = \/.fromEither(form.fold(Left.apply, Right.apply))
  }

}
