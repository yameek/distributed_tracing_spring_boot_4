We’ll keep sdk-playground/demo-app and demo-client trimmed down to the simplest possible surface—only the DTOs/endpoints/config that the current annotation processor requires—so they stay stable testbeds while api-sdk evolves. As new processor features land, we’ll layer them into the playground incrementally (updating the automation script if needed) before raising PRs to dev.
Just let me know when a new capability is ready to try and I’ll wire it into the demo pair for validation.
## prompt to test sdk 
added some new fetures in api-sdk 
so adapt them to demo-app and later use demo client to test the flow 
except for sdk-playground dir all other is your source of truth 
do only update inside sdk-playground and generate a report in md 
what was the changes of new features 
did it work or not ?
what are the chagnges are need to make on demo app to make it work
also what changes are need to make to use them demo client 
if failed :
what could be the reason of this error and how can solve it 