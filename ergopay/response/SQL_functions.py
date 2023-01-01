import sqlite3


def write_to_response_table(db_name, tableName, uuid, tx):
    connection = sqlite3.connect(r'data/' + f'{db_name}.db')
    cursor = connection.cursor()
    command1 = f'CREATE TABLE IF NOT EXISTS {tableName}(uuid TEXT, tx TEXT)'
    cursor.execute(command1)
    cursor.execute(f"INSERT INTO {tableName} VALUES (?,?)",
                   (uuid, tx))
    connection.commit()
    connection.close()


def query_response_table(query):
    connection = sqlite3.connect(r'data/response.db')
    cursor = connection.cursor()
    sqlite_select_query = """SELECT * from response WHERE uuid = ?"""
    cursor.execute(sqlite_select_query, (query,))
    results = cursor.fetchone()
    connection.close()
    return results