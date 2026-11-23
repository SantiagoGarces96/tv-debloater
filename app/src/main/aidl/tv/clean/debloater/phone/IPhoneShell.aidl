package tv.clean.debloater.phone;

interface IPhoneShell {
    void destroy() = 16777114;
    String exec(String cmd) = 1;
}
