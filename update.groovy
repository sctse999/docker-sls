// Retrive all tags from repo
String cmd = "git ls-remote -t https://github.com/serverless/serverless.git"
def process = cmd.execute();

def versions = [];

process.text.eachLine() {
    def result = it=~ /v([0-9]*).([0-9]*).([0-9]*)/

    if (result.size() > 0) {
        // println result[0];
        def version = result[0][0];
        def majorVersion = result[0][1];
        def minorVersion = Integer.parseInt(result[0][2]);
        def patchVersion = result[0][3];
        println "v${majorVersion}.${minorVersion}.${patchVersion}"

        if (majorVersion > 0 && minorVersion >= 23) {
            versions << version;
        }
    }
}


versions = versions.unique()

versions.each() { version->

    def folder = new File(version);
    if (!folder.exists()) {
        "mkdir ${version}".execute().waitFor();
        String dockerfile = generateDockerfile(version);

        String filePath = "${version}/Dockerfile"
        def file = new File(filePath)
        file.newWriter().withWriter { w->
            w << dockerfile
        }

        println "cp $filePath ./Dockerfile"
        "cp $filePath ./Dockerfile".execute().waitFor();

        execute("git add .")
        execute("git commit -m '${version}'")
        execute("git tag -f ${version}");
        // execute("git push origin -f ${version}");
    } else {
        println "${version} directory exists, skipping";
    }
}

def execute(String cmd) {
    def proc = cmd.execute();
    proc.waitFor();
    println "$cmd: " + proc.text
}

def generateDockerfile(String version) {
    String templateFile = './Dockerfile.tmpl'
    String dockerfile = new File(templateFile).text
    dockerfile = dockerfile.replace('%%version%%', version)
    return dockerfile
}