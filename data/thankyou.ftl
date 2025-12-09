<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>Thank you!!</title>
	<link rel="stylesheet" href="css/candidates.css">
</head>
<body>
<div class="container flex flex-col align-center">
	<h1>Thank you for your vote!</h1>
	<main class="flex flex-wrap align-center">
		<div class="card">
			<div class="flex flex-col align-center">
				<img src="images/${candidate.photo}">
				<p>Thank you for the vote!</p>
				<p>Now ${candidate.name} has: ${percent}% votes</p>
			</div>
		</div>
		<a class="back flex align-center" href="/">back to main</a>
		<a class="back flex align-center" href="/votes" style="margin-top: 10px;">See all results</a>
	</main>
</div>
</body>
</html>