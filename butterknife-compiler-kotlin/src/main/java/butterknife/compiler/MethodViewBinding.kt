package butterknife.compiler

class MethodViewBinding(val name: String, var parameters: List<Parameter>, var required: Boolean,
                        var hasReturnValue: Boolean) : MemberViewBinding {

    override fun description() = "method '$name'"
}