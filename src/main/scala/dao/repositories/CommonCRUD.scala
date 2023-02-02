package dao.repositories

import zio.Task

trait CommonCRUD[T] {

  def create(entity: T): Task[T]
  def read(id: Long): Task[Option[T]]
  def update(entity: T): Task[Option[T]]
  def delete(id: Long): Task[Unit]
}
