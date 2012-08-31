#!/usr/bin/php
<?
$sites = array(
	"en" => "en/index.html",
	"de" => "de/index.html",
	"ru" => "ru/index.html"
);

$langs = explode(",", $_SERVER['HTTP_ACCEPT_LANGUAGE']);

foreach ($langs as $lang) {
	$lang = strtolower(substr(chop($lang), 0, 2));
	if (array_key_exists($lang, $sites)) {
		header("Location: ".$sites[$lang]);
		return;
	}
}

header("Location: ".$sites["en"]);
?>
