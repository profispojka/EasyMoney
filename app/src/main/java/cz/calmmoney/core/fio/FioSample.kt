package cz.calmmoney.core.fio

/** Ukázková Fio odpověď pro vyzkoušení importu bez živého tokenu (struktura = reálné API). */
object FioSample {
    val JSON: String = """
    {"accountStatement":{"info":{"accountId":"2200123456","bankId":"2010","currency":"CZK",
      "iban":"CZ7920100000002200123456","closingBalance":18540.50},
     "transactionList":{"transaction":[
       {"column22":{"value":31000001,"name":"ID pohybu","id":22},
        "column0":{"value":"2026-06-02+0200","name":"Datum","id":0},
        "column1":{"value":-459.00,"name":"Objem","id":1},
        "column14":{"value":"CZK","name":"Měna","id":14},
        "column2":{"value":"123456789","name":"Protiúčet","id":2},
        "column10":{"value":"Albert","name":"Název protiúčtu","id":10},
        "column16":{"value":"Nákup potravin","name":"Zpráva pro příjemce","id":16}},
       {"column22":{"value":31000002,"name":"ID pohybu","id":22},
        "column0":{"value":"2026-06-05+0200","name":"Datum","id":0},
        "column1":{"value":-1280.00,"name":"Objem","id":1},
        "column14":{"value":"CZK","name":"Měna","id":14},
        "column10":{"value":"Shell","name":"Název protiúčtu","id":10},
        "column16":{"value":"Tankování","name":"Zpráva pro příjemce","id":16}},
       {"column22":{"value":31000003,"name":"ID pohybu","id":22},
        "column0":{"value":"2026-06-09+0200","name":"Datum","id":0},
        "column1":{"value":-219.00,"name":"Objem","id":1},
        "column14":{"value":"CZK","name":"Měna","id":14},
        "column10":{"value":"Netflix","name":"Název protiúčtu","id":10},
        "column16":{"value":"Předplatné","name":"Zpráva pro příjemce","id":16}},
       {"column22":{"value":31000004,"name":"ID pohybu","id":22},
        "column0":{"value":"2026-06-12+0200","name":"Datum","id":0},
        "column1":{"value":42000.00,"name":"Objem","id":1},
        "column14":{"value":"CZK","name":"Měna","id":14},
        "column10":{"value":"Zaměstnavatel s.r.o.","name":"Název protiúčtu","id":10},
        "column5":{"value":"1234","name":"VS","id":5},
        "column16":{"value":"Mzda 06/2026","name":"Zpráva pro příjemce","id":16}}
     ]}}}
    """.trimIndent()
}
