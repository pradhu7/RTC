BEGIN{FS=":"}
{
    ga = sprintf("%s:%s", $1, $2)
    if (!arts[ga]) {
	arts[ga] = $4
        type[ga] = $3
    }
    else {
	printf("Artifact %s version declared more than once.  Using %s instead of %s\n", ga, arts[ga], $4)
    }
}
END{
    for (ga in arts) {
	split(ga, pieces)
	if (type[ga] == "pom")
	    printf("<dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version><type>pom</type></dependency>\n", pieces[1], pieces[2], arts[ga])
	else
	    printf("<dependency><groupId>%s</groupId><artifactId>%s</artifactId><version>%s</version></dependency>\n", pieces[1], pieces[2], arts[ga])
    }
}
