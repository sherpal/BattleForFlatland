package utils.database.models

import models.Role

final case class DBRole(roleId: String, roleName: String) {
  def role: Role = Role.roleByName(roleName)
}
