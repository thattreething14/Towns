package site.albaniacraft.towns.model


class Town(val name: String, val founder: String) {
    val members: MutableList<String> = mutableListOf()

    // Update the constructor to accept members
    constructor(name: String, founder: String, members: MutableList<String>) : this(name, founder) {
        this.members.addAll(members)
    }
    fun isMayor(playerName: String): Boolean {
        return founder == playerName
    }
    fun addMember(memberName: String) {
        if (!members.contains(memberName)) {
            members.add(memberName)
        }
    }

    fun removeMember(memberName: String) {
        members.remove(memberName)
    }
}
