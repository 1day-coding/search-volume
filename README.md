###The assignment
Implement a microservice with a single REST endpoint.
This endpoint should receive a single keyword as an input and should return a score for that
same exact keyword. The score should be in the range [0 → 100] and represent the
estimated search-volume (how often Amazon customers search for that exact keyword). A
score of 0 means that the keyword is practically never searched for, 100 means that this is
one of the hottest keywords in all of amazon.com right now.
Example API Request (note: 65 is not the real score)\
`
REQUEST GET http://localhost:8080/estimate?keyword=iphone+charger 
`   
`
RESPONSE
{
“Keyword”:”iphone charger”,
“score”:65
}
`

The only hint we get from Amazon about which keywords are important comes from their AJAX autocomplete API. 
You can reverse-engineer how it works by typing text into the search-box on amazon.com.

The task is to extract a possibly precise estimation from the data from the
autocomplete API by understanding the logic that Amazon applies when choosing the
suggested keywords, reverse engineer it and find a clever algorithm that utilizes the
information to compute the search-volume score. The results should be computed live.
The new microservice should have an SLA of 10 seconds for a request round-trip.

Important assumptions:
- Any keyword with a relevant search-volume can be returned by the API.
- For any search input, Amazon will only return up to 10 keywords, that have an exact prefix-match with the input.
- The order of the 10 returned keywords is comparatively insignificant.
- Whenever the API is called, it operates in 2 steps:
  1. Seek: Get all known keywords that match the prefix and create a Candidate-Set
  2. Sort/Return: Sort the Candidate-Set by search-volume and return the top 10 results.

###Solution assumptions
1. Full search match based on original keyword is not representative (ex.: "iphone" returns "iphone" in the list);
2. Only partial search will return trending information containing the relative search volumes (ex.: "ip" returns "iphone" in the list);
3. The shortest substring of the original keyword will return the match with the biggest weight;
4. The order is comparatively insignificant - that means: 
 a) exact match based on original keyword doesn't show the trend;
 b) the order is changed depending on partial search length (so we have to take into account all possible partial searches);
5. Prefix-matched words are misleading as they can point to another products, we should look at the match of partial search with original keyword.

###Algorithm
1. Create an array of weights depending on how long is the partial search string.
Assumption: The sum of all weights is 100, the biggest weight is for single-char partial search.
    Calculate number of chars for all partial searches (ex.: 3 chars + 2 chars + 1 chars = 3 * (3 + 1) / 2 ): 
        `int sumOfPartialCharCounts = maxPartialCharCount * (maxPartialCharCount + 1) / 2;`
    Calculate weight per 1 char: 
        `double weightCoefficient = 100.0 / sumOfPartialCharCounts;`
Fill in the array of weights, so that 1 char search has the biggest weight.
Example: for keyword "canon", partial searches ["c", "ca", "can", "cano"] the weight array is [40.0, 30.0, 20.0, 10.0]

2. Get autocomplete list for each partial search, notice the position in the list.
Example: for keyword "canon" partial searches would be "c", "ca", "can", "cano".

3. Sum up partial search scores - this would be the final score.
Example: for keyword "canon", partial searches ["c", "ca", "can", "cano"] and top position in the autocomplete list we would have:\
`40.0 / 1 + 30.0 / 1 + 20.0 / 1 + 10.0 / 1 => 100.0`
but usually it would be like: 
`40.0 / 4 + 30.0 / 3 + 20.0 / 2 + 10.0 / 1` for "c", "ca", "can", "cano" respectively.


In other words, the logic of this algorithm is: we take into account only search by part of the keyword.
The less part of the original input is found - the more searched is the input.
For instance, if I search "iphone charger" and get "iphone charger" - it is not surprising, but if I search the string 
"iphone" and still get "iphone charger" - it means this string is searched very often.

So, basically, we remove one character from the input and check, whether we have original input in the response or not.

The final score is sum of every partial search. The score of every partial search is based upon the assumptions:
1) the smaller the input is, the more weight it has (the word is very trending);
2) the place in the list returned - if it's higher, it has better search volume.

The weight is counted in such a way: 100 is the total weight. We found, how much weight we have per char position 
(we find number of positions like sum of natural numbers from 1 to length-1 )      
Then we divide 100 by this number and have weightCoefficient per 1 position. 

And multiply position and this number to have an array of weights,
then use this information along with position in the autocomplete list for each partial search and sum all up.

###About the hint
The hint is correct in the meaning: 
 - exact match based on original keyword doesn't show the trend;
 - the order is changed depending on partial search length (so we have to take into account all possible partial searches);
 - Amazon could (potentially) raise the keyword in the list to increase its sales.

###How precise is solution
It's potentially relatively precise because it takes into account weighted partial search with ranked match.
Why? Because if the keyword is seen at the top position in the list, when only partial search is requested - it says about how 
often customers request this product.
But it doesn't say anything about how it is positioned against other keywords, searched on the Amazon, as we have no information 
about it. 

###The API page
The API page is accessible by the following link:
`http://localhost:8080/swagger-ui/index.html`