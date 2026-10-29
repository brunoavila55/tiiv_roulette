package br.com.nasorte

import java.util.Random

/** Each eligible ID has one ticket, even when names are equal. */
object DrawPool {
    fun eligible(ids: List<String>, drawn: Set<String>, noRepeat: Boolean): List<String> =
        ids.filter { !noRepeat || it !in drawn }

    fun pick(eligible: List<String>, random: Random): String {
        check(eligible.isNotEmpty()) { "Nenhum item disponível" }
        return eligible[random.nextInt(eligible.size)]
    }
}
