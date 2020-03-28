package models.users

sealed trait Role extends PartiallyOrdered[Role] {

  final def name: String = toString // concrete members are case objects

  /** List of roles this role is directly under. */
  def under: List[Role]

  def allUnder: List[Role] = under ++ under.flatMap(_.allUnder)

  override def tryCompareTo[B >: Role](that: B)(implicit evidence$1: AsPartiallyOrdered[B]): Option[Int] = that match {
    case that: Role =>
      if (that == this) Some(0)
      else if (that.allUnder.contains(this)) Some(1)
      else if (this.allUnder.contains(that)) Some(-1)
      else None
    case _ => None
  }
}

object Role {

  case object SuperUser extends Role {
    def under: List[Role] = Nil
  }

  case object Moderator extends Role {
    def under: List[Role] = List(SuperUser)
  }

  case object SimpleUser extends Role {
    def under: List[Role] = List(Moderator)
  }

  def roles: List[Role] = List(SuperUser, Moderator, SimpleUser)

  def roleByName(roleName: String): Role = roles.find(_.name == roleName).get

}
