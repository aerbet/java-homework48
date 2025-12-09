<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>Who's winning?</title>
	<link rel="stylesheet" href="css/candidates.css">
</head>
<body>
<div class="container flex flex-col align-center">
	<h1>Who's winning?</h1>
	<main class="flex flex-wrap align-center">

		<#list candidates as candidate>
		<div class="card">
			<div class="flex flex-col align-center">
				<img src="images/${candidate.photo}">
				<p>${candidate.name}</p>
				<p>Votes: ${candidate.getPercent(totalVotes)}% (${candidate.votes} votes)</p>
			</div>
		</div>
	</#list>

	<a class="back flex align-center" href="/">back to main</a>
	</main>
</div>
</body>
</html>