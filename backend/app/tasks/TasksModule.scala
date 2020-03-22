package tasks

import play.api.inject.{SimpleModule, _}

final class TasksModule extends SimpleModule(bind[AddRolesAndSuperUserInDBIfNotExist].toSelf.eagerly())
