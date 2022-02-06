package payconiq.example.stockmanagerrest.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import payconiq.example.stockmanagerrest.entities.Stock;
import payconiq.example.stockmanagerrest.services.StockService;

import java.security.InvalidParameterException;
import java.util.List;

@RestController
@ExposesResourceFor(Stock.class)
@RequestMapping("/api/stocks")
public class StockController {

    @Autowired
    private EntityLinks entityLinks;

    @Autowired
    private StockService stockService;


    // GET /api/stocks (get a list of stocks)
    @Operation(summary = "Get list of stocks (Ordered by Id in ascending order), you must specify page number and page size")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful Operation",
                    content = { @Content(mediaType = "application/json") }),
            @ApiResponse(responseCode = "400", description = "Invalid parameters supplied",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content)})
    @GetMapping(produces = { "application/json" })
    public ResponseEntity<CollectionModel<Stock>> getStocks(@Parameter(description = "Page index (start from 0)") @RequestParam(value = "page") Integer page,@Parameter(description = "Number of records per page") @RequestParam(value = "pageSize") Integer pageSize) {

        // Retrieve requested portion of stocks from database
        List<Stock> stockList = stockService.listStocks(page,pageSize);

        // Add self Link to each record as url for retrieving the record
        for (Stock stock : stockList) {
            Link recordSelfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(StockController.class)
                    .getStockById(stock.getID())).withSelfRel();
            stock.add(recordSelfLink);
        }
        CollectionModel<Stock> resources = CollectionModel.of(stockList);
        // selfLink to api according to HATEOS
        Link selfLink = entityLinks.linkToCollectionResource(Stock.class);
        resources.add(selfLink);

        // Send data to client as a response
        return new ResponseEntity(EntityModel.of(resources),HttpStatus.OK);
    }


    //GET /api/stocks/1 (get one stock from the list)
    @Operation(summary = "Get a stock by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Found the stock",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Stock.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Stock not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content) })
    @GetMapping(value = "/{stockId}", produces = { "application/json" })
    public ResponseEntity<EntityModel<Stock>> getStockById(@Parameter(description = "id of stock to be retrieved")  @PathVariable int stockId){

        // selfLink to api according to HATEOS
        Link selfLink = entityLinks.linkToItemResource(Stock.class, stockId);
        // Retrieve requested stock from database
        Stock stock = stockService.getStockById(stockId);
        //Check whether the stock exist or not
        if(stock!=null) {
            EntityModel<Stock> resource = EntityModel.of(stock);
            resource.add(selfLink);
            // Send data to client as a response
            return new ResponseEntity(EntityModel.of(resource),HttpStatus.OK);
        }

        // If no stock is found send 404 status code as response
        return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }


    // POST /api/stocks (create a stock)
    @Operation(summary = "Create a new stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Stock created",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Stock.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Stock not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content) })
    @PostMapping
    public ResponseEntity<EntityModel<Stock>> createStock(@RequestBody Stock stock){

        // Create and Save stock in database
        Stock storedStock = stockService.createStock(stock);

        // Check whether the stock is saved or not
        if(stock!=null) {
            // selfLink to api that retrieves the stock according to HATEOS
            Link recordSelfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(StockController.class)
                    .getStockById(stock.getID())).withSelfRel();
            stock.add(recordSelfLink);
            // Send created stock with 201 status code to client
            return new ResponseEntity(EntityModel.of(storedStock), HttpStatus.CREATED);
        }
        // If no stock is saved send 304 status code
        return new ResponseEntity(null, HttpStatus.NOT_MODIFIED);
    }


    //PATCH /api/stocks/1 (update the price of a single stock)
    @Operation(summary = "Update a stock by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Stock updated",
                    content = { @Content(mediaType = "application/json",
                            schema = @Schema(implementation = Stock.class)) }),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Stock not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content) })
    @PatchMapping(value = "/{stockId}", produces = { "application/json" })
    public ResponseEntity<EntityModel<Stock>> updateStock(@Parameter(description = "id of stock to be updated")  @PathVariable int stockId,@Parameter(description = "stock updated information")  @RequestBody Stock stock){

        // Update stock in the database
        Stock storedStock = stockService.updateStock(stockId,stock);

        // Check whether the stock exist and is updated or not
        if(storedStock!=null) {
            // selfLink to api that retrieves the stock according to HATEOS
            Link recordSelfLink = WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder.methodOn(StockController.class)
                    .getStockById(stock.getID())).withSelfRel();
            stock.add(recordSelfLink);
            // Send updated stock with 202 status code to client
            return new ResponseEntity(EntityModel.of(storedStock), HttpStatus.ACCEPTED);
        }

        // If no stock is found send 404 status code as response
        return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }

    // DELETE/api/stocks/1 (delete a single stock)
    @Operation(summary = "Delete a stock by its id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Stock deleted",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid id supplied",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Stock not found",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Error",
                    content = @Content) })
    @DeleteMapping(value = "/{stockId}")
    public ResponseEntity<EntityModel<Stock>> deleteStock(@Parameter(description = "id of stock to be deleted")  @PathVariable int stockId){

        // Delete the stock by its Id and check for the result
        if(stockService.deleteStock(stockId))
            // Sent empty response with 202 status code
            return new ResponseEntity(null, HttpStatus.ACCEPTED);
        else
            // If no stock is found send 404 status code as response
            return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }




        // Handling exceptions related to user input
        @ExceptionHandler({ MissingServletRequestParameterException.class, InvalidParameterException.class })
        public ResponseEntity<String> handleUserInputException() {

            return new ResponseEntity("Bad Request", HttpStatus.BAD_REQUEST);
        }

    // Handling runtime exception
    @ExceptionHandler({ RuntimeException.class })
    public ResponseEntity<String> handleRuntimeException() {

        return new ResponseEntity("Bad Request", HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
