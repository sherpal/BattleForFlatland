package utils.database.models

import models.users.Role

final case class DBRole(roleId: String, roleName: String) {
  def role: Role = Role.roleByName(roleName)
}
