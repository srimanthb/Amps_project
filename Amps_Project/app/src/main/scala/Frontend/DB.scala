package Frontend

import java.sql.{Connection, DriverManager}
import scala.util.Using

object DB {

  val url = "jdbc:sqlserver://localhost:1433;databaseName=Frontend;encrypt=false"
  val user = "sa"
  private val password = "Srimanth@9"

  private def connection: Connection = DriverManager.getConnection(url, user, password)

  def insertTrade(accountNo:String, symbol: String, quantity: String) : Int ={
    Using.resource(connection){
      conn =>
        val insert =
          """
            insert into Trade(accountNo,symbol, quantity) values(?,?,?)
            """
        val stmt = conn.prepareStatement(insert)
        stmt.setString(1,accountNo)
        stmt.setString(2,symbol)
        stmt.setString(3,quantity)
        stmt.executeUpdate()

    }

  }


  connection.close()


}


