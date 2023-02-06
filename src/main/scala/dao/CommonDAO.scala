package dao

import io.getquill.{EntityQuery, Quoted}
import zio.Task

trait CommonDAO[T] {
  val quoted: Quoted[EntityQuery[T]]
  def create(entity: T): Task[T]
  def read(id: Long): Task[Option[T]]
  def update(entity: T): Task[Option[T]]
  def delete(id: Long): Task[Unit]
}
